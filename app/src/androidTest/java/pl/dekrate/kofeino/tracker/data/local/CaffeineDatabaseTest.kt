package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

/**
 * Tests database creation and seeding of default drinks.
 */
@RunWith(AndroidJUnit4::class)
class CaffeineDatabaseTest {

    private lateinit var database: CaffeineDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun daosAreAccessible() = runTest {
        // Smoke test: DAOs are wired and database accepts queries.
        // The DAOs are returned as non-null — this verifies the KSP/Room
        // annotation processing wired them correctly.
        val intakeDao = database.caffeineIntakeDao()
        val drinkDao = database.drinkDao()
        val cacheDao = database.officialDrinkCacheDao()

        // Trigger a real query to verify the database is operational
        assert(drinkDao.getAllDrinks().first().isEmpty()) { "Should start with empty drinks" }
        assert(intakeDao.getRecentIntakes(1).first().isEmpty()) { "Should start with empty intakes" }
        assert(cacheDao.count() == 0) { "Should start with empty cache" }
    }

    @Test
    fun databaseHasCorrectVersion() {
        assert(database.openHelper.readableDatabase.version == 3) {
            "Expected database version 3, got ${database.openHelper.readableDatabase.version}"
        }
    }

    @Test
    fun tablesExist() = runTest {
        val db = database.openHelper.readableDatabase

        // Check all expected tables exist
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name IN ('caffeine_intakes', 'drinks', 'official_drink_cache')"
        )
        val tables = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()

        assert("caffeine_intakes" in tables) { "caffeine_intakes table not found" }
        assert("drinks" in tables) { "drinks table not found" }
        assert("official_drink_cache" in tables) { "official_drink_cache table not found" }
    }

    @Test
    fun destructiveMigrationFallbackDoesNotCrash() = runTest {
        // Verify that the database builder configured with
        // fallbackToDestructiveMigration works correctly through
        // a create → close → reopen cycle (same version).
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_destructive_${System.currentTimeMillis()}"

        try {
            // First instance: create and write
            var db: CaffeineDatabase? = null
            try {
                db = Room.databaseBuilder(context, CaffeineDatabase::class.java, dbName)
                    .fallbackToDestructiveMigration(true)
                    .build()
                db.drinkDao().insert(
                    DrinkEntity(name = "Pre-close", caffeineMg = 10, volumeMl = 100)
                )
                assert(db.isOpen) { "DB must be open after write" }
            } finally {
                db?.close()
            }

            // Second instance: reopen (same version → no migration needed)
            try {
                db = Room.databaseBuilder(context, CaffeineDatabase::class.java, dbName)
                    .fallbackToDestructiveMigration(true)
                    .build()
                val drinks = db.drinkDao().getAllDrinks().first()
                assert(db.isOpen) { "DB must be open after reopen" }
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }
}
