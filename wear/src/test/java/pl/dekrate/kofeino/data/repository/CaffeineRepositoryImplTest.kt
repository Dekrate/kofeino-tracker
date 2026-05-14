package pl.dekrate.kofeino.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class CaffeineRepositoryImplTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var intakeDao: CaffeineIntakeDao
    private lateinit var drinkDao: DrinkDao
    private lateinit var repository: CaffeineRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        intakeDao = database.caffeineIntakeDao()
        drinkDao = database.drinkDao()
        repository = CaffeineRepositoryImpl(intakeDao, drinkDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- Intake tests ---

    @Test
    fun `addIntake should insert and return in date list`() = runTest {
        val intake = createIntake(caffeineMg = 63)
        repository.addIntake(intake)

        val now = startOfDay(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(63, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTotalCaffeineForDate should sum only that date entries`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 63))
        repository.addIntake(createIntake(caffeineMg = 95))

        val now = startOfDay(System.currentTimeMillis())
        repository.getTotalCaffeineForDate(now).test {
            assertEquals(158, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getIntakesForDate should ignore entries from other days`() = runTest {
        val yesterday = System.currentTimeMillis() - 86_400_000L
        repository.addIntake(createIntake(caffeineMg = 200, timestamp = yesterday))
        repository.addIntake(createIntake(caffeineMg = 50))

        val now = startOfDay(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(50, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }

        val yesterdayStart = startOfDay(yesterday)
        repository.getIntakesForDate(yesterdayStart).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(200, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll should remove all intakes`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 100))
        repository.clearAll()

        val now = startOfDay(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateIntake should persist changes`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 50))
        val now = startOfDay(System.currentTimeMillis())
        val intake = repository.getIntakesForDate(now).let {
            // find the intake via test
            var result: CaffeineIntake? = null
            repository.getIntakesForDate(now).test {
                result = awaitItem().first()
                cancelAndIgnoreRemainingEvents()
            }
            result!!
        }
        repository.updateIntake(intake.copy(caffeineMg = 100))

        repository.getIntakesForDate(now).test {
            assertEquals(100, awaitItem().first().caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteIntake should remove entry`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 50))
        val now = startOfDay(System.currentTimeMillis())
        var intake: CaffeineIntake? = null
        repository.getIntakesForDate(now).test {
            intake = awaitItem().first()
            cancelAndIgnoreRemainingEvents()
        }

        repository.deleteIntake(intake!!)
        repository.getIntakesForDate(now).test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty repository should emit zero total and empty list`() = runTest {
        val now = startOfDay(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        repository.getTotalCaffeineForDate(now).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple intakes should be ordered by timestamp descending`() = runTest {
        val now = System.currentTimeMillis()
        repository.addIntake(createIntake(caffeineMg = 10, timestamp = now - 10_000))
        repository.addIntake(createIntake(caffeineMg = 20, timestamp = now))
        repository.addIntake(createIntake(caffeineMg = 30, timestamp = now - 5_000))

        val today = startOfDay(now)
        repository.getIntakesForDate(today).test {
            val list = awaitItem()
            assertEquals(listOf(20, 30, 10), list.map { it.caffeineMg })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Drink tests ---

    @Test
    fun `addDrink should persist and return drink ID`() = runTest {
        val drink = DrinkEntity(name = "Testowy napój", caffeineMg = 50, volumeMl = 200)
        val id = repository.addDrink(drink)
        assertTrue("Drink ID should be positive", id > 0)
    }

    @Test
    fun `getDrinkById should return added drink`() = runTest {
        val drink = DrinkEntity(name = "Mocna kawa", caffeineMg = 120, volumeMl = 150)
        val id = repository.addDrink(drink)
        val loaded = repository.getDrinkById(id)
        assertEquals("Mocna kawa", loaded?.name)
        assertEquals(120, loaded?.caffeineMg)
    }

    @Test
    fun `getAllDrinks should return all drinks`() = runTest {
        repository.addDrink(DrinkEntity(name = "A", caffeineMg = 10, volumeMl = 100))
        repository.addDrink(DrinkEntity(name = "B", caffeineMg = 20, volumeMl = 200))

        val now = startOfDay(System.currentTimeMillis())
        repository.getAllDrinks().test {
            val list = awaitItem()
            assertTrue(list.size >= 2) // may include seeded defaults
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateDrink should persist name change`() = runTest {
        val id = repository.addDrink(DrinkEntity(name = "Old", caffeineMg = 50, volumeMl = 200))
        repository.updateDrink(repository.getDrinkById(id)!!.copy(name = "New"))

        val updated = repository.getDrinkById(id)
        assertEquals("New", updated?.name)
    }

    @Test
    fun `deleteDrink should remove the drink`() = runTest {
        val id = repository.addDrink(DrinkEntity(name = "ToDelete", caffeineMg = 50, volumeMl = 200))
        val drink = repository.getDrinkById(id)!!
        repository.deleteDrink(drink)
        val after = repository.getDrinkById(id)
        assertEquals(null, after)
    }

    // --- Helpers ---

    private fun createIntake(
        caffeineMg: Int = 63,
        timestamp: Long = System.currentTimeMillis()
    ): CaffeineIntake {
        return CaffeineIntake(
            drinkName = "test_drink",
            caffeineMg = caffeineMg,
            volumeMl = 250,
            timestamp = timestamp
        )
    }

    private fun startOfDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
