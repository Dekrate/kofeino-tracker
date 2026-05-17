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
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake

@RunWith(AndroidJUnit4::class)
class CaffeineIntakeDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: CaffeineIntakeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.caffeineIntakeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val intake = CaffeineIntake(
            drinkName = "Espresso",
            caffeineMg = 63,
            volumeMl = 30,
            timestamp = 1_000_000L
        )
        val id = dao.insert(intake)

        val retrieved = dao.getIntakeById(id)
        assert(retrieved != null) { "Expected intake, got null" }
        assert(retrieved!!.drinkName == "Espresso")
        assert(retrieved.caffeineMg == 63)
        assert(retrieved.volumeMl == 30)
    }

    @Test
    fun insertAndGetByDate() = runTest {
        // Use deterministic timestamps 3 days apart to avoid DST/midnight edge cases
        val recentTs = 1_000_000_000L // ~2001
        val oldTs = recentTs - 86400000L * 3 // 3 days earlier

        dao.insert(CaffeineIntake(drinkName = "Recent", caffeineMg = 95, volumeMl = 250, timestamp = recentTs))
        dao.insert(CaffeineIntake(drinkName = "Old", caffeineMg = 47, volumeMl = 250, timestamp = oldTs))

        // Window covering only the "recent" entry
        val startOfDay = recentTs - 1000L
        val endOfDay = recentTs + 1000L

        val intakes = dao.getIntakesByDate(startOfDay, endOfDay).first()
        assert(intakes.size == 1) { "Expected 1 intake in window, got ${intakes.size}" }
        assert(intakes[0].drinkName == "Recent")
    }

    @Test
    fun updateChangesValues() = runTest {
        val id = dao.insert(CaffeineIntake(drinkName = "Old", caffeineMg = 50, volumeMl = 100, timestamp = 1L))

        dao.update(CaffeineIntake(id = id, drinkName = "Updated", caffeineMg = 100, volumeMl = 200, timestamp = 1L))

        val retrieved = dao.getIntakeById(id)
        assert(retrieved!!.drinkName == "Updated")
        assert(retrieved.caffeineMg == 100)
        assert(retrieved.volumeMl == 200)
    }

    @Test
    fun deleteRemovesEntry() = runTest {
        val id = dao.insert(CaffeineIntake(drinkName = "Test", caffeineMg = 10, volumeMl = 50, timestamp = 1L))

        dao.delete(CaffeineIntake(id = id, drinkName = "Test", caffeineMg = 10, volumeMl = 50, timestamp = 1L))

        val retrieved = dao.getIntakeById(id)
        assert(retrieved == null) { "Expected null after delete, got $retrieved" }
    }

    @Test
    fun getTotalCaffeineByDate() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(CaffeineIntake(drinkName = "A", caffeineMg = 63, volumeMl = 30, timestamp = now))
        dao.insert(CaffeineIntake(drinkName = "B", caffeineMg = 95, volumeMl = 250, timestamp = now))

        val startOfDay = now - 86400000L
        val endOfDay = now + 1000L
        val total = dao.getTotalCaffeineByDate(startOfDay, endOfDay).first()

        assert(total == 158) { "Expected 158mg total, got $total" }
    }

    @Test
    fun getRecentIntakes() = runTest {
        dao.insert(CaffeineIntake(drinkName = "Old", caffeineMg = 10, volumeMl = 100, timestamp = 1000L))
        dao.insert(CaffeineIntake(drinkName = "New", caffeineMg = 20, volumeMl = 200, timestamp = 2000L))
        dao.insert(CaffeineIntake(drinkName = "Newest", caffeineMg = 30, volumeMl = 300, timestamp = 3000L))

        val recent = dao.getRecentIntakes(2).first()
        assert(recent.size == 2) { "Expected 2 recent intakes, got ${recent.size}" }
        assert(recent[0].drinkName == "Newest") { "Expected 'Newest' first, got '${recent[0].drinkName}'" }
    }

    @Test
    fun deleteAllClearsEverything() = runTest {
        dao.insert(CaffeineIntake(drinkName = "A", caffeineMg = 10, volumeMl = 100, timestamp = 1L))
        dao.insert(CaffeineIntake(drinkName = "B", caffeineMg = 20, volumeMl = 200, timestamp = 2L))

        dao.deleteAll()

        val all = dao.getRecentIntakes(100).first()
        assert(all.isEmpty()) { "Expected empty after deleteAll, got ${all.size}" }
    }

    @Test
    fun deleteOlderThan() = runTest {
        val now = 1000L
        dao.insert(CaffeineIntake(drinkName = "Old", caffeineMg = 10, volumeMl = 100, timestamp = 100L))
        dao.insert(CaffeineIntake(drinkName = "Recent", caffeineMg = 20, volumeMl = 200, timestamp = 2000L))

        dao.deleteOlderThan(500L)

        val remaining = dao.getRecentIntakes(100).first()
        assert(remaining.size == 1) { "Expected 1 remaining, got ${remaining.size}" }
        assert(remaining[0].drinkName == "Recent")
    }
}
