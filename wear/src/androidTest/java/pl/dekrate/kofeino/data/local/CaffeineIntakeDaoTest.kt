package pl.dekrate.kofeino.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class CaffeineIntakeDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: CaffeineIntakeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java).build()
        dao = database.caffeineIntakeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(createIntake(caffeineMg = 63))

        val retrieved = dao.getIntakeById(id)
        assertNotNull(retrieved)
        assertEquals(63, retrieved!!.caffeineMg)
        assertEquals("Espresso", retrieved.drinkName)
    }

    @Test
    fun insertReturnsPositiveId() = runTest {
        val id = dao.insert(createIntake())
        assertTrue("Generated ID should be positive", id > 0)
    }

    @Test
    fun getIntakesByDateReturnsTodayIntakes() = runTest {
        val now = System.currentTimeMillis()
        val dayBounds = todayBounds()

        dao.insert(createIntake(caffeineMg = 63, timestamp = now))
        dao.insert(createIntake(caffeineMg = 95, timestamp = now))

        val intakes = dao.getIntakesByDate(dayBounds.first, dayBounds.second).first()
        assertEquals(2, intakes.size)
    }

    @Test
    fun getIntakesByDateOrdersByTimestampDescending() = runTest {
        val now = System.currentTimeMillis()
        val dayBounds = todayBounds()

        dao.insert(createIntake(caffeineMg = 10, timestamp = now - 10_000))
        dao.insert(createIntake(caffeineMg = 20, timestamp = now))
        dao.insert(createIntake(caffeineMg = 30, timestamp = now - 5_000))

        val intakes = dao.getIntakesByDate(dayBounds.first, dayBounds.second).first()
        assertEquals(listOf(20, 30, 10), intakes.map { it.caffeineMg })
    }

    @Test
    fun getIntakesByDateExcludesOtherDays() = runTest {
        val today = todayBounds()
        val yesterday = yesterdayBounds()

        dao.insert(createIntake(caffeineMg = 50, timestamp = today.first + 1000))
        dao.insert(createIntake(caffeineMg = 200, timestamp = yesterday.first + 1000))

        val todayIntakes = dao.getIntakesByDate(today.first, today.second).first()
        assertEquals(1, todayIntakes.size)
        assertEquals(50, todayIntakes[0].caffeineMg)

        val yesterdayIntakes = dao.getIntakesByDate(yesterday.first, yesterday.second).first()
        assertEquals(1, yesterdayIntakes.size)
        assertEquals(200, yesterdayIntakes[0].caffeineMg)
    }

    @Test
    fun getIntakesByDateEmptyForFutureDate() = runTest {
        dao.insert(createIntake())

        val futureStart = System.currentTimeMillis() + 86_400_000L * 30
        val futureEnd = futureStart + 86_400_000L
        val intakes = dao.getIntakesByDate(futureStart, futureEnd).first()
        assertTrue("Expected empty for future date", intakes.isEmpty())
    }

    @Test
    fun updateIntake() = runTest {
        val id = dao.insert(createIntake(caffeineMg = 50))

        dao.update(dao.getIntakeById(id)!!.copy(caffeineMg = 100))

        val updated = dao.getIntakeById(id)
        assertEquals(100, updated!!.caffeineMg)
    }

    @Test
    fun updateIntakeDrinkName() = runTest {
        val id = dao.insert(createIntake(caffeineMg = 50))

        dao.update(dao.getIntakeById(id)!!.copy(drinkName = "Latte"))

        val updated = dao.getIntakeById(id)
        assertEquals("Latte", updated!!.drinkName)
    }

    @Test
    fun deleteIntake() = runTest {
        val id = dao.insert(createIntake())
        assertNotNull(dao.getIntakeById(id))

        dao.delete(dao.getIntakeById(id)!!)

        assertNull(dao.getIntakeById(id))
    }

    @Test
    fun deleteAll() = runTest {
        dao.insert(createIntake())
        dao.insert(createIntake(caffeineMg = 95))

        dao.deleteAll()

        val today = todayBounds()
        val intakes = dao.getIntakesByDate(today.first, today.second).first()
        assertTrue("Expected empty after deleteAll", intakes.isEmpty())
    }

    @Test
    fun getTotalCaffeineByDate() = runTest {
        val now = System.currentTimeMillis()
        val today = todayBounds()

        dao.insert(createIntake(caffeineMg = 63, timestamp = now))
        dao.insert(createIntake(caffeineMg = 95, timestamp = now))

        val total = dao.getTotalCaffeineByDate(today.first, today.second).first()
        assertEquals(158, total)
    }

    @Test
    fun getTotalCaffeineByDateExcludesOtherDays() = runTest {
        val today = todayBounds()
        val yesterday = yesterdayBounds()

        dao.insert(createIntake(caffeineMg = 100, timestamp = today.first + 1000))
        dao.insert(createIntake(caffeineMg = 200, timestamp = yesterday.first + 1000))

        val todayTotal = dao.getTotalCaffeineByDate(today.first, today.second).first()
        assertEquals(100, todayTotal)

        val yesterdayTotal = dao.getTotalCaffeineByDate(yesterday.first, yesterday.second).first()
        assertEquals(200, yesterdayTotal)
    }

    @Test
    fun getTotalCaffeineByDateEmptyReturnsZero() = runTest {
        val futureStart = System.currentTimeMillis() + 86_400_000L * 365
        val futureEnd = futureStart + 86_400_000L
        val total = dao.getTotalCaffeineByDate(futureStart, futureEnd).first()
        assertEquals(0, total)
    }

    @Test
    fun getIntakeByIdNonExistent() = runTest {
        assertNull(dao.getIntakeById(99999L))
    }

    @Test
    fun getIntakeByIdReturnsNullAfterDelete() = runTest {
        val id = dao.insert(createIntake(caffeineMg = 50))
        assertEquals(50, dao.getIntakeById(id)!!.caffeineMg)

        dao.delete(dao.getIntakeById(id)!!)

        assertNull(dao.getIntakeById(id))
    }

    // --- Helpers ---

    private fun createIntake(
        caffeineMg: Int = 63,
        timestamp: Long = System.currentTimeMillis()
    ): CaffeineIntake {
        return CaffeineIntake(
            drinkName = "Espresso",
            caffeineMg = caffeineMg,
            volumeMl = 30,
            timestamp = timestamp
        )
    }

    private fun todayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun yesterdayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }
}
