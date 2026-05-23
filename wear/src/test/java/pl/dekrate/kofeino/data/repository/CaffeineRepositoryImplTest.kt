package pl.dekrate.kofeino.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.sync.RealTimeSyncService
import io.mockk.mockk
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
    private val realTimeSyncService: RealTimeSyncService = mockk(relaxed = true)
    private lateinit var repository: CaffeineRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        intakeDao = database.caffeineIntakeDao()
        drinkDao = database.drinkDao()
        repository = CaffeineRepositoryImpl(intakeDao, drinkDao, realTimeSyncService)
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

        val now = millisToLocalDate(System.currentTimeMillis())
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

        val now = millisToLocalDate(System.currentTimeMillis())
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

        val now = millisToLocalDate(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(50, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }

        val yesterdayStart = millisToLocalDate(yesterday)
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

        val now = millisToLocalDate(System.currentTimeMillis())
        repository.getIntakesForDate(now).test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateIntake should persist changes`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 50))
        val now = millisToLocalDate(System.currentTimeMillis())
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
        val now = millisToLocalDate(System.currentTimeMillis())
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
        val now = millisToLocalDate(System.currentTimeMillis())
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

        val today = millisToLocalDate(now)
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

        val now = millisToLocalDate(System.currentTimeMillis())
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

    // ===== DST safety tests =====

    @Test
    fun `dayBounds should cover 25h on DST fall-back day`() = runTest {
        // Symulacja "cofnięcia" zegara (fall back) — 2025-10-26 w strefie EU
        // W Polsce zmiana: 03:00 → 02:00, dzień ma 25h
        val fallBackNoon = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.OCTOBER, 26, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        repository.addIntake(createIntake(caffeineMg = 50, timestamp = fallBackNoon))
        // Dodaj intake o 02:30 CET (po cofnięciu, ale wciąż ten sam dzień kalendarzowy)
        val afterFallBack = java.util.Calendar.getInstance().apply {
            set(2025, java.util.Calendar.OCTOBER, 26, 2, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        repository.addIntake(createIntake(caffeineMg = 30, timestamp = afterFallBack))

        val dayStart = testDayStart(fallBackNoon)
        repository.getIntakesForDate(dayStart).test {
            val list = awaitItem()
            assertEquals("Both intakes on same DST day", 2, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dayBounds should handle DST spring-forward correctly`() = runTest {
        // Symulacja "przeskoku" (spring forward) — 2026-03-29 w strefie EU
        // W Polsce zmiana: 02:00 → 03:00, dzień ma 23h
        val springForwardNoon = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.MARCH, 29, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        repository.addIntake(createIntake(caffeineMg = 100, timestamp = springForwardNoon))
        // Dzień wcześniej o 23:00 — powinien być w INNYM dniu
        val prevDay = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.MARCH, 28, 23, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        repository.addIntake(createIntake(caffeineMg = 200, timestamp = prevDay))

        val dayStart = testDayStart(springForwardNoon)
        repository.getIntakesForDate(dayStart).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(100, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dayBounds should not overlap between consecutive days`() = runTest {
        val calendar = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JUNE, 15, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val day1 = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val day2 = calendar.timeInMillis

        repository.addIntake(createIntake(caffeineMg = 10, timestamp = day1))
        repository.addIntake(createIntake(caffeineMg = 20, timestamp = day2))

        val start1 = testDayStart(day1)
        val start2 = testDayStart(day2)

        // Dzień 1
        repository.getIntakesForDate(start1).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(10, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }

        // Dzień 2
        repository.getIntakesForDate(start2).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(20, list[0].caffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== getIntakeById tests =====

    @Test
    fun `getIntakeById should return added intake`() = runTest {
        val intake = createIntake(caffeineMg = 75)
        val id = repository.addIntake(intake)

        val loaded = repository.getIntakeById(id)
        assertEquals(75, loaded?.caffeineMg)
    }

    @Test
    fun `getIntakeById should return null for non-existent id`() = runTest {
        val loaded = repository.getIntakeById(99999L)
        assertEquals(null, loaded)
    }

    @Test
    fun `getIntakeById should return null after deletion`() = runTest {
        val intake = createIntake(caffeineMg = 50)
        val id = repository.addIntake(intake)

        val beforeDelete = repository.getIntakeById(id)
        assertEquals(50, beforeDelete?.caffeineMg)

        repository.deleteIntake(beforeDelete!!)
        val afterDelete = repository.getIntakeById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun `getIntakeById should return correct intake when multiple exist`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 10))
        repository.addIntake(createIntake(caffeineMg = 20))
        repository.addIntake(createIntake(caffeineMg = 30))

        val now = millisToLocalDate(System.currentTimeMillis())
        val intakes = repository.getIntakesForDate(now).let {
            var result = listOf<CaffeineIntake>()
            repository.getIntakesForDate(now).test {
                result = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            result
        }

        for (intake in intakes) {
            val loaded = repository.getIntakeById(intake.id)
            assertEquals(intake.caffeineMg, loaded?.caffeineMg)
            assertEquals(intake.drinkName, loaded?.drinkName)
        }
    }

    // --- Helpers ---

    /**
     * Replikuje logikę CaffeineRepositoryImpl.dayBounds dla testów.
     * Zwraca początek dnia (północ) dla podanego timestampu.
     */
    private fun testDayStart(millis: Long): LocalDate {
        return millisToLocalDate(millis)
    }

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

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // ===== Edge case tests =====

    @Test
    fun `addIntake with zero caffeine should persist`() = runTest {
        val id = repository.addIntake(createIntake(caffeineMg = 0))
        assertTrue("ID should be positive", id > 0)

        val loaded = repository.getIntakeById(id)
        assertEquals(0, loaded?.caffeineMg)
    }

    @Test
    fun `addDrink with zero caffeine should persist`() = runTest {
        val id = repository.addDrink(DrinkEntity(name = "Zero Caffeine", caffeineMg = 0, volumeMl = 100))
        assertTrue("ID should be positive", id > 0)

        val loaded = repository.getDrinkById(id)
        assertEquals(0, loaded?.caffeineMg)
    }

    @Test
    fun `getTotalCaffeineForDate with no intakes should emit zero`() = runTest {
        val now = millisToLocalDate(System.currentTimeMillis())
        repository.getTotalCaffeineForDate(now).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getIntakesForDate with future date should return empty`() = runTest {
        repository.addIntake(createIntake(caffeineMg = 50))

        val futureDate = millisToLocalDate(System.currentTimeMillis() + 86_400_000L * 365)
        repository.getIntakesForDate(futureDate).test {
            assertEquals(emptyList<CaffeineIntake>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
