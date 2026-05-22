package pl.dekrate.kofeino.tracker.data.backup

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes and deserialises [BackupData] to/from JSON string.
 *
 * ## Design
 * - **Versioned schema**: the `version` field is always checked before full
 *   deserialisation. Unknown / future versions are rejected early.
 * - **Forward-compat**: unknown fields in the JSON are silently ignored,
 *   allowing newer backup files to be partially imported by older app versions
 *   as long as the version is [supported][BackupData.MIN_SUPPORTED_VERSION].
 * - **Single responsibility**: this class only handles JSON conversions; it
 *   does not know about files, content resolvers, or Room.
 */
@Singleton
class BackupSerializer @Inject constructor() {
    /** Dedicated [Gson] instance for backup serialisation. Separate from the
     *  network-layer Gson to allow different config (e.g. pretty-printing for
     *  human-readable backup files in the future). */
    private val gson: Gson = Gson()
    /**
     * Serialise [data] to a compact JSON string.
     *
     * @throws Exception if Gson serialisation fails (should not happen for
     *                   well-formed data).
     */
    fun serialize(data: BackupData): String {
        Timber.d("Serializing backup v%s with %d intakes, %d drinks",
            data.version, data.intakes.size, data.drinks.size)
        return gson.toJson(data)
    }

    /**
     * Deserialize a JSON string into [BackupData].
     *
     * ## Validation
     * 1. Parse the JSON tree to extract the `version` field **before** full
     *    deserialisation.
     * 2. Reject if `version < [MIN_SUPPORTED_VERSION]` (too old).
     * 3. Reject if `version > [CURRENT_VERSION]` (too new — we don't know the
     *    schema yet).
     * 4. Deserialise into [BackupData] — extra JSON fields are silently dropped.
     *
     * @throws BackupVersionException if the version is unsupported.
     * @throws Exception if the JSON is malformed.
     */
    fun deserialize(json: String): BackupData {
        val root = JsonParser.parseString(json).asJsonObject
        val version = extractVersion(root)

        if (version < BackupData.MIN_SUPPORTED_VERSION) {
            throw BackupVersionException(
                "Backup version $version is too old. " +
                    "Minimum supported version is ${BackupData.MIN_SUPPORTED_VERSION}."
            )
        }
        if (version > BackupData.CURRENT_VERSION) {
            throw BackupVersionException(
                "Backup version $version is too new. " +
                    "This app only supports up to version ${BackupData.CURRENT_VERSION}. " +
                    "Please update the app to restore this backup."
            )
        }

        val data = gson.fromJson(root, BackupData::class.java)
        Timber.d("Deserialized backup v%s with %d intakes, %d drinks",
            data.version, data.intakes.size, data.drinks.size)
        return data
    }

    /**
     * Extract the `version` field from a raw JSON root object.
     *
     * @throws Exception if the field is missing or not a number / string.
     */
    private fun extractVersion(root: JsonObject): Int {
        val element = root.get("version")
            ?: throw BackupVersionException("Backup file is missing the 'version' field.")
        return try {
            element.asInt
        } catch (e: NumberFormatException) {
            throw BackupVersionException("Invalid 'version' field: '${element.asString}'")
        }
    }

    /**
     * Create a [BackupData] from domain model lists, stamping the current
     * version and timestamp.
     */
    fun createBackup(
        intakes: List<BackupIntake>,
        drinks: List<BackupDrink>,
        settings: BackupSettings
    ): BackupData {
        return BackupData(
            version = BackupData.CURRENT_VERSION,
            exportedAt = Instant.now().toString(),
            intakes = intakes,
            drinks = drinks,
            settings = settings
        )
    }
}

/**
 * Thrown when a backup file has an unsupported version.
 */
class BackupVersionException(message: String) : Exception(message)
