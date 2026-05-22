package pl.dekrate.kofeino.tracker.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for [BackupManager].
 *
 * Tests the orchestration logic with mocked dependencies:
 * - [CaffeineRepository] for data access
 * - [BackupSerializer] for JSON (real instance — we want to verify actual
 *   serialisation roundtrips)
 * - [BackupConflictResolver] (real instance)
 * - [DataStorePreferences] for settings
 * - [ContentResolver] for SAF I/O
 */
class BackupManagerTest {

    private lateinit var repository: CaffeineRepository
    private lateinit var preferences: DataStorePreferences
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var backupManager: BackupManager
    private lateinit var uri: Uri

    private val serializer = BackupSerializer()
    private val conflictResolver = BackupConflictResolver()

    @Before
    fun setUp() {
        repository = mockk()
        preferences = mockk()
        context = mockk()
        contentResolver = mockk()
        uri = mockk()

        every { context.contentResolver } returns contentResolver
        every { uri.toString() } returns "content://backup/test-backup.json"

        backupManager = BackupManager(
            repository = repository,
            serializer = serializer,
            conflictResolver = conflictResolver,
            preferences = preferences,
            context = context
        )
    }

    // ------------------------------------------------------------------
    // 1. Export success
    // ------------------------------------------------------------------

    @Test
    fun `exportBackup writes correct JSON via contentResolver`() = runTest {
        val intakes = listOf(
            CaffeineIntake(id = 1, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000L)
        )
        val drinks = listOf(
            DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30, isDefault = true)
        )

        coEvery { repository.getAllIntakesSnapshot() } returns intakes
        coEvery { repository.getAllDrinksSnapshot() } returns drinks
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream

        val result = backupManager.exportBackup(uri)

        assertEquals(1, result.intakeCount)
        assertEquals(1, result.drinkCount)

        // Verify JSON was written
        val json = outputStream.toString(Charsets.UTF_8)
        assertTrue(json.contains("Espresso"))
        assertTrue(json.contains("version"))
        assertTrue(json.contains("exportedAt"))
    }

    @Test
    fun `exportBackup with empty data produces valid JSON`() = runTest {
        coEvery { repository.getAllIntakesSnapshot() } returns emptyList()
        coEvery { repository.getAllDrinksSnapshot() } returns emptyList()
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream

        val result = backupManager.exportBackup(uri)

        assertEquals(0, result.intakeCount)
        assertEquals(0, result.drinkCount)

        val json = outputStream.toString(Charsets.UTF_8)
        @Suppress("SdCardPath")
        assertTrue(json.contains("\"intakes\":[]"))
        assertTrue(json.contains("\"drinks\":[]"))
    }

    // ------------------------------------------------------------------
    // 2. Export failure
    // ------------------------------------------------------------------

    @Test(expected = BackupIOException::class)
    fun `exportBackup throws when contentResolver returns null`() = runTest {
        coEvery { repository.getAllIntakesSnapshot() } returns emptyList()
        coEvery { repository.getAllDrinksSnapshot() } returns emptyList()
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        every { contentResolver.openOutputStream(uri) } returns null

        backupManager.exportBackup(uri)
    }

    // ------------------------------------------------------------------
    // 3. Import success — all new
    // ------------------------------------------------------------------

    @Test
    fun `importBackup reads JSON and imports all new data`() = runTest {
        val json = """
            {
                "version": 1,
                "exportedAt": "2026-05-22T10:00:00Z",
                "intakes": [
                    {"id": 1, "drinkId": null, "drinkName": "Espresso", "caffeineMg": 63, "volumeMl": 30, "timestamp": 1000}
                ],
                "drinks": [
                    {"id": 1, "name": "Latte", "caffeineMg": 63, "volumeMl": 250, "isDefault": false}
                ],
                "settings": {"language": "pl", "themeMode": "dark", "notifLiveEnabled": true,
                    "notifMorningEnabled": false, "notifRegularEnabled": false, "notifEveningEnabled": false}
            }
        """.trimIndent()

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())
        coEvery { repository.getAllIntakeIds() } returns emptyList()
        coEvery { repository.getAllDrinkNames() } returns emptyList()
        coEvery { repository.bulkInsertIntakes(any()) } just Runs
        coEvery { repository.bulkInsertDrinks(any()) } just Runs

        val result = backupManager.importBackup(uri)

        assertEquals(1, result.intakesImported)
        assertEquals(0, result.intakesSkipped)
        assertEquals(1, result.drinksImported)
        assertEquals(0, result.drinksSkipped)
    }

    // ------------------------------------------------------------------
    // 4. Import — partial conflict
    // ------------------------------------------------------------------

    @Test
    fun `importBackup skips intakes and drinks that already exist`() = runTest {
        val json = """
            {
                "version": 1,
                "exportedAt": "2026-05-22T10:00:00Z",
                "intakes": [
                    {"id": 10, "drinkName": "Existing", "caffeineMg": 50, "volumeMl": 100, "timestamp": 1},
                    {"id": 20, "drinkName": "New", "caffeineMg": 75, "volumeMl": 200, "timestamp": 2}
                ],
                "drinks": [
                    {"name": "Espresso", "caffeineMg": 63, "volumeMl": 30},
                    {"name": "Matcha", "caffeineMg": 30, "volumeMl": 200}
                ],
                "settings": {}
            }
        """.trimIndent()

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())
        coEvery { repository.getAllIntakeIds() } returns listOf(10L)
        coEvery { repository.getAllDrinkNames() } returns listOf("Espresso")
        coEvery { repository.bulkInsertIntakes(any()) } just Runs
        coEvery { repository.bulkInsertDrinks(any()) } just Runs

        val result = backupManager.importBackup(uri)

        assertEquals(1, result.intakesImported) // only id=20 is new
        assertEquals(1, result.intakesSkipped)   // id=10 exists
        assertEquals(1, result.drinksImported)   // Matcha is new
        assertEquals(1, result.drinksSkipped)    // Espresso exists
    }

    // ------------------------------------------------------------------
    // 5. Import with settings
    // ------------------------------------------------------------------

    @Test
    fun `importBackup applies settings when importSettings is true`() = runTest {
        val json = """
            {
                "version": 1,
                "exportedAt": "now",
                "intakes": [],
                "drinks": [],
                "settings": {"language": "pl", "themeMode": "dark", "notifLiveEnabled": true,
                    "notifMorningEnabled": true, "notifRegularEnabled": false, "notifEveningEnabled": false}
            }
        """.trimIndent()

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())
        coEvery { repository.getAllIntakeIds() } returns emptyList()
        coEvery { repository.getAllDrinkNames() } returns emptyList()
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false
        coEvery { preferences.setLanguage(any()) } just Runs
        coEvery { preferences.setThemeMode(any()) } just Runs
        coEvery { preferences.setNotificationLiveEnabled(any()) } just Runs
        coEvery { preferences.setNotificationMorningEnabled(any()) } just Runs
        coEvery { preferences.setNotificationRegularEnabled(any()) } just Runs

        val result = backupManager.importBackup(uri, importSettings = true)

        assertTrue(result.settingsImported)
        coVerify { preferences.setLanguage("pl") }
        coVerify { preferences.setThemeMode("dark") }
    }

    @Test
    fun `importBackup does NOT apply settings when importSettings is false`() = runTest {
        val json = """
            {
                "version": 1,
                "exportedAt": "now",
                "intakes": [],
                "drinks": [],
                "settings": {"language": "pl", "themeMode": "dark",
                    "notifLiveEnabled": true, "notifMorningEnabled": false,
                    "notifRegularEnabled": false, "notifEveningEnabled": false}
            }
        """.trimIndent()

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())
        coEvery { repository.getAllIntakeIds() } returns emptyList()
        coEvery { repository.getAllDrinkNames() } returns emptyList()

        val result = backupManager.importBackup(uri, importSettings = false)

        assertEquals(0, result.intakesImported)
        assertEquals(0, result.drinksImported)
    }

    // ------------------------------------------------------------------
    // 6. Import — version rejection
    // ------------------------------------------------------------------

    @Test(expected = BackupVersionException::class)
    fun `importBackup rejects unsupported version`() = runTest {
        val json = """{"version":0,"exportedAt":"now","intakes":[],"drinks":[],"settings":{}}"""

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())

        backupManager.importBackup(uri)
    }

    @Test(expected = BackupVersionException::class)
    fun `importBackup rejects too-new version`() = runTest {
        val json = """{"version":999,"exportedAt":"now","intakes":[],"drinks":[],"settings":{}}"""

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())

        backupManager.importBackup(uri)
    }

    // ------------------------------------------------------------------
    // 7. Import — I/O error
    // ------------------------------------------------------------------

    @Test(expected = BackupIOException::class)
    fun `importBackup throws when contentResolver returns null input stream`() = runTest {
        every { contentResolver.openInputStream(uri) } returns null

        backupManager.importBackup(uri)
    }

    // ------------------------------------------------------------------
    // 8. Verification: Domain → Backup DTO mapping
    // ------------------------------------------------------------------

    @Test
    fun `exportBackup maps domain models correctly`() = runTest {
        val intake = CaffeineIntake(id = 5, drinkId = 3, drinkName = "Cappuccino",
            caffeineMg = 75, volumeMl = 200, timestamp = 5000L)
        val drink = DrinkEntity(id = 10, name = "Cappuccino", caffeineMg = 75,
            volumeMl = 200, isDefault = false)

        coEvery { repository.getAllIntakesSnapshot() } returns listOf(intake)
        coEvery { repository.getAllDrinksSnapshot() } returns listOf(drink)
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream

        backupManager.exportBackup(uri)

        val json = outputStream.toString(Charsets.UTF_8)
        assertTrue(json.contains("Cappuccino"))
        assertTrue(json.contains("75"))
        assertTrue(json.contains("200"))
        assertTrue(json.contains("5000"))
    }

    // ------------------------------------------------------------------
    // 9. Import calls conflict resolver correctly
    // ------------------------------------------------------------------

    @Test
    fun `importBackup passes correct data to conflict resolver`() = runTest {
        val json = """
            {
                "version": 1,
                "exportedAt": "now",
                "intakes": [{"id": 1, "drinkName": "A", "caffeineMg": 50, "volumeMl": 100, "timestamp": 1}],
                "drinks": [{"name": "B", "caffeineMg": 60, "volumeMl": 200}],
                "settings": {}
            }
        """.trimIndent()

        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())
        coEvery { repository.getAllIntakeIds() } returns listOf(1L)
        coEvery { repository.getAllDrinkNames() } returns listOf("B")
        coEvery { repository.bulkInsertIntakes(any()) } just Runs
        coEvery { repository.bulkInsertDrinks(any()) } just Runs

        val result = backupManager.importBackup(uri)

        // Both should be skipped due to conflicts
        assertEquals(0, result.intakesImported)
        assertEquals(1, result.intakesSkipped)
        assertEquals(0, result.drinksImported)
        assertEquals(1, result.drinksSkipped)
    }
}
