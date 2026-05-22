package pl.dekrate.kofeino.tracker.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import timber.log.Timber
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the Backup / Restore flow for the phone app.
 *
 * ## Responsibilities
 * - **Export**: gather all intakes + drinks + settings from the repository,
 *   serialise via [BackupSerializer], and write the JSON to a SAF-chosen URI.
 * - **Import**: read JSON from a SAF-chosen URI, deserialise, resolve conflicts
 *   via [BackupConflictResolver], and persist the new data.
 *
 * ## Design
 * - Uses **Constructor Injection** for all dependencies — easy to test.
 * - The [importBackup] method is designed to be called from a coroutine
 *   (e.g. ViewModel scope) and propagates errors to the caller.
 * - SAF I/O runs on the calling thread — the caller should use
 *   [kotlinx.coroutines.Dispatchers.IO] if needed.
 *
 * ## Thread-safety
 * This class is **not** thread-safe by itself. The caller (ViewModel /
 * WorkManager) should ensure sequential access or use a single coroutine
 * context.
 */
@Singleton
class BackupManager @Inject constructor(
    private val repository: CaffeineRepository,
    private val serializer: BackupSerializer,
    private val conflictResolver: BackupConflictResolver,
    private val preferences: DataStorePreferences,
    @ApplicationContext private val context: Context
) {
    /**
     * Export all data to a JSON file at [uri] (user-chosen via SAF).
     *
     * ## Flow
     * 1. Snapshot all intakes, drinks, and settings from the local DB.
     * 2. Serialise to JSON via [BackupSerializer].
     * 3. Write the JSON string to the SAF URI via [ContentResolver].
     *
     * @param uri The SAF [Uri] returned by `ActivityResultContracts.CreateDocument`.
     * @return [ExportResult] with counts of exported entities.
     * @throws Exception if I/O fails.
     */
    suspend fun exportBackup(uri: Uri): ExportResult {
        Timber.d("Starting backup export to %s", uri)

        // 1. Gather data — snapshot (non-observable) reads
        val intakes = repository.getAllIntakesSnapshot()
        val drinks = repository.getAllDrinksSnapshot()
        val settings = readSettings()

        val backupData = serializer.createBackup(
            intakes = intakes.map { it.toBackupIntake() },
            drinks = drinks.map { it.toBackupDrink() },
            settings = settings
        )

        val json = serializer.serialize(backupData)

        // 2. Write to SAF URI
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                writer.write(json)
            }
        } ?: throw BackupIOException("Failed to open output stream for $uri")

        Timber.d("Backup export complete: %d intakes, %d drinks",
            backupData.intakes.size, backupData.drinks.size)

        return ExportResult(
            intakeCount = backupData.intakes.size,
            drinkCount = backupData.drinks.size
        )
    }

    /**
     * Import data from a JSON file at [uri] (user-chosen via SAF).
     *
     * ## Flow
     * 1. Read the JSON string from SAF.
     * 2. Deserialise via [BackupSerializer].
     * 3. Snapshot existing local IDs/names for conflict resolution.
     * 4. Resolve conflicts via [BackupConflictResolver].
     * 5. Persist new intakes and drinks to Room.
     *
     * ## Additive import
     * See [BackupConflictResolver] for the detailed conflict strategy.
     * Settings import is optional — controlled by [importSettings].
     *
     * @param uri The SAF [Uri] returned by `ActivityResultContracts.OpenDocument`.
     * @param importSettings Whether to also apply the backed-up settings.
     * @return [ImportResult] with counts of imported / skipped entities.
     * @throws BackupVersionException if the backup version is unsupported.
     * @throws Exception if I/O or deserialisation fails.
     */
    suspend fun importBackup(uri: Uri, importSettings: Boolean = false): ImportResult {
        Timber.d("Starting backup import from %s (importSettings=%s)", uri, importSettings)

        // 1. Read JSON from SAF URI
        val json = readJsonFromUri(uri)

        // 2. Deserialise
        val backupData = serializer.deserialize(json)

        // 2b. Validate deserialised data before touching the database
        validateBackupData(backupData)

        // 3. Snapshot existing data for conflict resolution
        val existingIntakeIds = repository.getAllIntakeIds().toSet()
        val existingDrinkNames = repository.getAllDrinkNames().toSet()

        // 4. Resolve conflicts
        val resolution = conflictResolver.resolve(
            importedIntakes = backupData.intakes,
            importedDrinks = backupData.drinks,
            existingIntakeIds = existingIntakeIds,
            existingDrinkNames = existingDrinkNames
        )

        // 5. Persist atomically — if either insert fails, both roll back
        var intakesImported = 0
        var drinksImported = 0

        if (resolution.intakesToInsert.isNotEmpty() || resolution.drinksToInsert.isNotEmpty()) {
            repository.importAllAtomic(resolution.intakesToInsert, resolution.drinksToInsert)
            intakesImported = resolution.intakesToInsert.size
            drinksImported = resolution.drinksToInsert.size
        }

        // 6. Apply settings if requested
        if (importSettings) {
            applySettings(backupData.settings)
        }

        Timber.d("Backup import complete: %d intakes (+%d skipped), %d drinks (+%d skipped)",
            intakesImported, resolution.intakesSkipped,
            drinksImported, resolution.drinksSkipped)

        return ImportResult(
            intakesImported = intakesImported,
            intakesSkipped = resolution.intakesSkipped,
            drinksImported = drinksImported,
            drinksSkipped = resolution.drinksSkipped,
            settingsImported = importSettings
        )
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /** Read the full content of a SAF URI as a UTF-8 string, preserving line breaks. */
    private fun readJsonFromUri(uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                reader.readText()
            }
        } ?: throw BackupIOException("Failed to open input stream for $uri")
    }

    /** Snapshot current settings into a [BackupSettings] value. */
    private fun readSettings(): BackupSettings {
        return BackupSettings(
            language = preferences.getLanguage(),
            themeMode = preferences.getThemeMode(),
            notifLiveEnabled = preferences.isNotificationLiveEnabled(),
            notifMorningEnabled = preferences.isNotificationMorningEnabled(),
            notifRegularEnabled = preferences.isNotificationRegularEnabled(),
            notifEveningEnabled = preferences.isNotificationEveningEnabled()
        )
    }

    /**
     * Basic validation of deserialised backup data before persisting.
     * Catches obviously-corrupted data (negative values, null names)
     * that Gson would accept silently.
     */
    private fun validateBackupData(data: BackupData) {
        data.intakes.forEach { intake ->
            require(intake.caffeineMg >= 0) { "Intake has negative caffeineMg: ${intake.caffeineMg}" }
            require(intake.volumeMl > 0) { "Intake has non-positive volumeMl: ${intake.volumeMl}" }
            require(intake.drinkName.isNotBlank()) { "Intake has blank drinkName" }
        }
        data.drinks.forEach { drink ->
            require(drink.caffeineMg >= 0) { "Drink has negative caffeineMg: ${drink.caffeineMg}" }
            require(drink.volumeMl > 0) { "Drink has non-positive volumeMl: ${drink.volumeMl}" }
            require(drink.name.isNotBlank()) { "Drink has blank name" }
        }
        Timber.d("Backup data validation passed: %d intakes, %d drinks",
            data.intakes.size, data.drinks.size)
    }

    /** Apply backed-up settings to local preferences. */
    private suspend fun applySettings(settings: BackupSettings) {
        if (settings.language != preferences.getLanguage()) {
            preferences.setLanguage(settings.language)
        }
        if (settings.themeMode != preferences.getThemeMode()) {
            preferences.setThemeMode(settings.themeMode)
        }
        if (settings.notifLiveEnabled != preferences.isNotificationLiveEnabled()) {
            preferences.setNotificationLiveEnabled(settings.notifLiveEnabled)
        }
        if (settings.notifMorningEnabled != preferences.isNotificationMorningEnabled()) {
            preferences.setNotificationMorningEnabled(settings.notifMorningEnabled)
        }
        if (settings.notifRegularEnabled != preferences.isNotificationRegularEnabled()) {
            preferences.setNotificationRegularEnabled(settings.notifRegularEnabled)
        }
        if (settings.notifEveningEnabled != preferences.isNotificationEveningEnabled()) {
            preferences.setNotificationEveningEnabled(settings.notifEveningEnabled)
        }
        Timber.d("Settings imported — language=%s theme=%s", settings.language, settings.themeMode)
    }
}

// ---------------------------------------------------------------------------
// Extension functions: Domain model → Backup DTO
// ---------------------------------------------------------------------------

internal fun CaffeineIntake.toBackupIntake() = BackupIntake(
    id = id,
    drinkId = drinkId,
    drinkName = drinkName,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    timestamp = timestamp
)

internal fun DrinkEntity.toBackupDrink() = BackupDrink(
    id = id,
    name = name,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    isDefault = isDefault
)

// ---------------------------------------------------------------------------
// Result types
// ---------------------------------------------------------------------------

data class ExportResult(
    val intakeCount: Int,
    val drinkCount: Int
)

data class ImportResult(
    val intakesImported: Int,
    val intakesSkipped: Int,
    val drinksImported: Int,
    val drinksSkipped: Int,
    val settingsImported: Boolean
)

/**
 * Thrown when a SAF I/O operation fails (e.g. user cancelled, file not found).
 */
class BackupIOException(message: String, cause: Throwable? = null) : Exception(message, cause)
