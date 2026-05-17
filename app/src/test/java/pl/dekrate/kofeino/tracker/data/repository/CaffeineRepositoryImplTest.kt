package pl.dekrate.kofeino.tracker.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

class CaffeineRepositoryImplTest {

    private val intakeDao: CaffeineIntakeDao = mockk()
    private val drinkDao: DrinkDao = mockk()
    private lateinit var repository: CaffeineRepositoryImpl

    private val sampleIntake = CaffeineIntake(
        id = 1,
        drinkName = "Espresso",
        caffeineMg = 63,
        volumeMl = 30,
        timestamp = 1_000_000L
    )

    private val sampleDrink = DrinkEntity(
        id = 1,
        name = "Espresso",
        caffeineMg = 63,
        volumeMl = 30,
        isDefault = true
    )

    @Before
    fun setUp() {
        repository = CaffeineRepositoryImpl(intakeDao, drinkDao)
    }

    // --- Intake tests ---

    @Test
    fun `addIntake delegates to dao and returns id`() = runTest {
        coEvery { intakeDao.insert(any()) } returns 42L

        val result = repository.addIntake(sampleIntake)

        assert(result == 42L) { "Expected 42L, got $result" }
        coVerify { intakeDao.insert(sampleIntake) }
    }

    @Test
    fun `updateIntake delegates to dao`() = runTest {
        coEvery { intakeDao.update(any()) } returns Unit

        repository.updateIntake(sampleIntake)

        coVerify { intakeDao.update(sampleIntake) }
    }

    @Test
    fun `deleteIntake delegates to dao`() = runTest {
        coEvery { intakeDao.delete(any()) } returns Unit

        repository.deleteIntake(sampleIntake)

        coVerify { intakeDao.delete(sampleIntake) }
    }

    @Test
    fun `getIntakeById delegates to dao`() = runTest {
        coEvery { intakeDao.getIntakeById(1L) } returns sampleIntake

        val result = repository.getIntakeById(1L)

        assert(result == sampleIntake) { "Expected intake, got $result" }
        coVerify { intakeDao.getIntakeById(1L) }
    }

    @Test
    fun `getIntakeById returns null when not found`() = runTest {
        coEvery { intakeDao.getIntakeById(999L) } returns null

        val result = repository.getIntakeById(999L)

        assert(result == null) { "Expected null, got $result" }
    }

    @Test
    fun `getIntakesForDate returns flow from dao`() = runTest {
        val intakes = listOf(sampleIntake)
        coEvery { intakeDao.getIntakesByDate(any(), any()) } returns flowOf(intakes)

        repository.getIntakesForDate(1_000_000L).test {
            val emitted = awaitItem()
            assert(emitted == intakes) { "Expected intakes, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTotalCaffeineForDate returns flow from dao`() = runTest {
        coEvery { intakeDao.getTotalCaffeineByDate(any(), any()) } returns flowOf(126)

        repository.getTotalCaffeineForDate(1_000_000L).test {
            val emitted = awaitItem()
            assert(emitted == 126) { "Expected 126, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll delegates to dao`() = runTest {
        coEvery { intakeDao.deleteAll() } returns Unit

        repository.clearAll()

        coVerify { intakeDao.deleteAll() }
    }

    @Test
    fun `getRecentIntakes delegates to dao`() = runTest {
        val intakes = listOf(sampleIntake)
        coEvery { intakeDao.getRecentIntakes(50) } returns flowOf(intakes)

        repository.getRecentIntakes(50).test {
            val emitted = awaitItem()
            assert(emitted == intakes) { "Expected intakes, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Drink tests ---

    @Test
    fun `getAllDrinks delegates to dao`() = runTest {
        val drinks = listOf(sampleDrink)
        coEvery { drinkDao.getAllDrinks() } returns flowOf(drinks)

        repository.getAllDrinks().test {
            val emitted = awaitItem()
            assert(emitted == drinks) { "Expected drinks, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getDrinkById delegates to dao`() = runTest {
        coEvery { drinkDao.getDrinkById(1L) } returns sampleDrink

        val result = repository.getDrinkById(1L)

        assert(result == sampleDrink) { "Expected drink, got $result" }
        coVerify { drinkDao.getDrinkById(1L) }
    }

    @Test
    fun `addDrink delegates to dao and returns id`() = runTest {
        coEvery { drinkDao.insert(any()) } returns 7L

        val result = repository.addDrink(sampleDrink)

        assert(result == 7L) { "Expected 7L, got $result" }
        coVerify { drinkDao.insert(sampleDrink) }
    }

    @Test
    fun `updateDrink delegates to dao`() = runTest {
        coEvery { drinkDao.update(any()) } returns Unit

        repository.updateDrink(sampleDrink)

        coVerify { drinkDao.update(sampleDrink) }
    }

    @Test
    fun `deleteDrink delegates to dao`() = runTest {
        coEvery { drinkDao.delete(any()) } returns Unit

        repository.deleteDrink(sampleDrink)

        coVerify { drinkDao.delete(sampleDrink) }
    }

    @Test
    fun `searchDrinks delegates to dao`() = runTest {
        val drinks = listOf(sampleDrink)
        coEvery { drinkDao.searchDrinks("Espresso") } returns flowOf(drinks)

        repository.searchDrinks("Espresso").test {
            val emitted = awaitItem()
            assert(emitted == drinks) { "Expected drinks, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
