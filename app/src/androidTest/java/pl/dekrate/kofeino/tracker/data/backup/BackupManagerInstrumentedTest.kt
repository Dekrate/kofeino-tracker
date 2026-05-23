package pl.dekrate.kofeino.tracker.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepositoryImpl
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Instrumented integration tests for [BackupManager].
 *
 * Uses a real Room in-memory database and real [CaffeineRepositoryImpl]
 * so the full export/import pipeline is exercised end-to-end.
 * SAF I/O is mocked via [ContentResolver].
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerInstrumentedTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var repository: CaffeineRepository
    private lateinit var backupManager: BackupManager
    private lateinit var serializer: BackupSerializer
    private lateinit var conflictResolver: BackupConflictResolver
    private lateinit var contentResolver: ContentResolver
    private lateinit var context: Context
    private lateinit var preferences: DataStorePreferences
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .build()

        val intakeDao = database.caffeineIntakeDao()
        val drinkDao = database.drinkDao()
        repository = CaffeineRepositoryImpl(intakeDao, drinkDao, database)
        serializer = BackupSerializer()
        conflictResolver = BackupConflictResolver()
        contentResolver = mockk()
        preferences = mockk()
        uri = mockk()

        every { context.contentResolver } returns contentResolver
        every { uri.toString() } returns "content://backup/test.json"

        backupManager = BackupManager(
            repository = repository,
            serializer = serializer,
            conflictResolver = conflictResolver,
            preferences = preferences,
            context = context
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------------------
    // 1. Full export → import cycle with real database
    // ------------------------------------------------------------------

    @Test
    fun fullExportImportCycle_roundtripsDataCorrectly() = runTest {
        // ── Arrange: populate database with test data ──
        repository.addIntake(CaffeineIntake(drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000L))
        repository.addIntake(CaffeineIntake(drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 2000L))
        repository.addDrink(DrinkEntity(name = "Espresso", caffeineMg = 63, volumeMl = 30, isDefault = true))
        repository.addDrink(DrinkEntity(name = "Latte", caffeineMg = 63, volumeMl = 250))

        // ── Export ──
        every { preferences.getLanguage() } returns "en"
        every { preferences.getThemeMode() } returns "system"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns false
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream

        val exportResult = backupManager.exportBackup(uri)
        assertEquals(2, exportResult.intakeCount)
        assertEquals(2, exportResult.drinkCount)

        val json = outputStream.toString(Charsets.UTF_8)
        assertTrue(json.contains("Espresso"))
        assertTrue(json.contains("Latte"))
        assertTrue(json.contains("version"))

        // ── Import into a fresh database and verify round-trip ──
        val freshDatabase = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .build()
        try {
            val freshIntakeDao = freshDatabase.caffeineIntakeDao()
            val freshDrinkDao = freshDatabase.drinkDao()
            val freshRepo = CaffeineRepositoryImpl(freshIntakeDao, freshDrinkDao, freshDatabase)

            // We need a fresh BackupManager that reads from our JSON string via contentResolver
            val freshContentResolver: ContentResolver = mockk()
            every { freshContentResolver.openInputStream(uri) } returns ByteArrayInputStream(json.toByteArray())

            // For import, the manager needs to query the FRESH DB for conflict resolution
            // (which is empty — no conflicts)
            val freshPreferences: DataStorePreferences = mockk()
            val freshContext: Context = mockk()
            every { freshContext.contentResolver } returns freshContentResolver
            every { freshContext.getString(any()) } returns ""

            val freshBackupManager = BackupManager(
                repository = freshRepo,
                serializer = serializer,
                conflictResolver = conflictResolver,
                preferences = freshPreferences,
                context = freshContext
            )

            // Import the backup JSON
            freshBackupManager.importBackup(uri, importSettings = false)

            // Verify the data was restored correctly
            val importedIntakes = freshRepo.getAllIntakesSnapshot()
            assertEquals(2, importedIntakes.size)
            assertTrue(importedIntakes.any { it.drinkName == "Espresso" })
            assertTrue(importedIntakes.any { it.drinkName == "Latte" })

            val importedDrinks = freshRepo.getAllDrinksSnapshot()
            assertEquals(2, importedDrinks.size)
            assertTrue(importedDrinks.any { it.name == "Espresso" })
            assertTrue(importedDrinks.any { it.name == "Latte" })
        } finally {
            freshDatabase.close()
        }
    }

    // ------------------------------------------------------------------
    // 2. Export preserves exact intake count and values
    // ------------------------------------------------------------------

    @Test
    fun exportContainsExactDataFromDatabase() = runTest {
        val now = 1000L
        repository.addIntake(CaffeineIntake(drinkName = "Test", caffeineMg = 50, volumeMl = 150, timestamp = now))

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
        val data = serializer.deserialize(json)

        assertEquals(1, data.intakes.size)
        assertEquals("Test", data.intakes[0].drinkName)
        assertEquals(50, data.intakes[0].caffeineMg)
        assertEquals(150, data.intakes[0].volumeMl)
        assertEquals(now, data.intakes[0].timestamp)
    }

    // ------------------------------------------------------------------
    // 3. Export with real data validates via repository snapshot
    // ------------------------------------------------------------------

    @Test
    fun repositorySnapshotReturnsAllData() = runTest {
        repository.addIntake(CaffeineIntake(drinkName = "A", caffeineMg = 10, volumeMl = 100, timestamp = 1L))
        repository.addIntake(CaffeineIntake(drinkName = "B", caffeineMg = 20, volumeMl = 200, timestamp = 2L))
        repository.addDrink(DrinkEntity(name = "Custom A", caffeineMg = 30, volumeMl = 150))

        val intakes = repository.getAllIntakesSnapshot()
        val drinks = repository.getAllDrinksSnapshot()

        assertEquals(2, intakes.size)
        assertEquals(1, drinks.size)
        assertTrue(intakes.any { it.drinkName == "A" })
        assertTrue(intakes.any { it.drinkName == "B" })
        assertTrue(drinks.any { it.name == "Custom A" })
    }

    // ------------------------------------------------------------------
    // 4. Serialized JSON can be deserialized back to matching data
    // ------------------------------------------------------------------

    @Test
    fun jsonRoundtripProducesMatchingBackupData() = runTest {
        repository.addIntake(CaffeineIntake(drinkName = "Matcha", caffeineMg = 30, volumeMl = 200, timestamp = 5000L))
        repository.addDrink(DrinkEntity(name = "Matcha", caffeineMg = 30, volumeMl = 200))

        every { preferences.getLanguage() } returns "pl"
        every { preferences.getThemeMode() } returns "dark"
        every { preferences.isNotificationLiveEnabled() } returns true
        every { preferences.isNotificationMorningEnabled() } returns true
        every { preferences.isNotificationRegularEnabled() } returns false
        every { preferences.isNotificationEveningEnabled() } returns false

        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream

        backupManager.exportBackup(uri)
        val json = outputStream.toString(Charsets.UTF_8)

        // Deserialize and verify
        val data = serializer.deserialize(json)
        assertEquals(1, data.intakes.size)
        assertEquals("Matcha", data.intakes[0].drinkName)
        assertEquals(1, data.drinks.size)
        assertEquals("Matcha", data.drinks[0].name)
        assertEquals("pl", data.settings.language)
        assertEquals("dark", data.settings.themeMode)
    }

    // ------------------------------------------------------------------
    // 5. Bulk insert works correctly
    // ------------------------------------------------------------------

    @Test
    fun bulkInsertIntakesInsertsAllItems() = runTest {
        val intakes = listOf(
            CaffeineIntake(drinkName = "X", caffeineMg = 10, volumeMl = 100, timestamp = 1L),
            CaffeineIntake(drinkName = "Y", caffeineMg = 20, volumeMl = 200, timestamp = 2L),
            CaffeineIntake(drinkName = "Z", caffeineMg = 30, volumeMl = 300, timestamp = 3L)
        )

        repository.bulkInsertIntakes(intakes)

        val snapshot = repository.getAllIntakesSnapshot()
        assertEquals(3, snapshot.size)
    }

    @Test
    fun bulkInsertDrinksInsertsAllItems() = runTest {
        val drinks = listOf(
            DrinkEntity(name = "Custom 1", caffeineMg = 50, volumeMl = 150),
            DrinkEntity(name = "Custom 2", caffeineMg = 75, volumeMl = 200)
        )

        repository.bulkInsertDrinks(drinks)

        val snapshot = repository.getAllDrinksSnapshot()
        assertEquals(2, snapshot.size)
    }
}
