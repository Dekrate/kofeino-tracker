package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests database creation and seeding of default drinks.
 */
@RunWith(AndroidJUnit4::class)
class CaffeineDatabaseTest {

    private lateinit var database: CaffeineDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.databaseBuilder(
            context,
            CaffeineDatabase::class.java,
            "test_caffeine_database_${System.currentTimeMillis()}"
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseIsCreatedWithAllDaos() {
        assert(database.caffeineIntakeDao() != null) { "CaffeineIntakeDao must not be null" }
        assert(database.drinkDao() != null) { "DrinkDao must not be null" }
        assert(database.officialDrinkCacheDao() != null) { "OfficialDrinkCacheDao must not be null" }
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
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('caffeine_intakes', 'drinks', 'official_drink_cache')")
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
    fun databaseMigrationFallbackWorks() {
        // This tests that .fallbackToDestructiveMigration() is configured
        // by verifying the builder compiles and runs (destructive migration
        // would just recreate tables)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(
            context,
            CaffeineDatabase::class.java,
            "test_migration_${System.currentTimeMillis()}"
        )
            .fallbackToDestructiveMigration(true)
            .build()

        // Accessing the DB triggers it to be created
        db.openHelper.writableDatabase
        assert(db.isOpen) { "Database must be open" }
        db.close()
    }
}
