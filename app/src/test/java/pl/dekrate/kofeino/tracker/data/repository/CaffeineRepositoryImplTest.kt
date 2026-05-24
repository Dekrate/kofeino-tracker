package pl.dekrate.kofeino.tracker.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.data.sync.RealTimeSyncService
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake as EntityCaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity as EntityDrinkEntity
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake as CommonCaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity as CommonDrinkEntity

class CaffeineRepositoryImplTest {

    private val intakeDao: CaffeineIntakeDao = mockk()
    private val drinkDao: DrinkDao = mockk()
    private val database: CaffeineDatabase = mockk()
    private val realTimeSyncService: RealTimeSyncService = mockk()
    private lateinit var repository: CaffeineRepositoryImpl

    private val sampleEntityIntake = EntityCaffeineIntake(
        id = 1,
        drinkName = "Espresso",
        caffeineMg = 63,
        volumeMl = 30,
        timestamp = 1_000_000L
    )

    private val sampleEntityDrink = EntityDrinkEntity(
        id = 1,
        name = "Espresso",
        caffeineMg = 63,
        volumeMl = 30,
        isDefault = true
    )

    @Before
    fun setUp() {
        repository = CaffeineRepositoryImpl(intakeDao, drinkDao, database, realTimeSyncService)
    }

    // --- Intake tests ---

    @Test
    fun `addIntake delegates to dao and returns id`() = runTest {
        coEvery { intakeDao.insert(any()) } returns 42L

        val commonIntake = sampleEntityIntake.toCommon()
        val result = repository.addIntake(commonIntake)

        assert(result == 42L) { "Expected 42L, got $result" }
        coVerify { intakeDao.insert(match { it.drinkName == "Espresso" && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `updateIntake delegates to dao`() = runTest {
        coEvery { intakeDao.update(any()) } returns Unit

        val commonIntake = sampleEntityIntake.toCommon()
        repository.updateIntake(commonIntake)

        coVerify { intakeDao.update(match { it.id == sampleEntityIntake.id && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `deleteIntake delegates to dao`() = runTest {
        coEvery { intakeDao.delete(any()) } returns Unit

        val commonIntake = sampleEntityIntake.toCommon()
        repository.deleteIntake(commonIntake)

        coVerify { intakeDao.delete(match { it.id == sampleEntityIntake.id && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `getIntakeById delegates to dao`() = runTest {
        coEvery { intakeDao.getIntakeById(1L) } returns sampleEntityIntake

        val result = repository.getIntakeById(1L)

        assert(result == sampleEntityIntake.toCommon()) { "Expected intake, got $result" }
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
        val intakes = listOf(sampleEntityIntake)
        coEvery { intakeDao.getIntakesByDate(any(), any()) } returns flowOf(intakes)

        repository.getIntakesForDate(LocalDate(2026, 1, 1)).test {
            val emitted = awaitItem()
            val expected = intakes.map { it.toCommon() }
            assert(emitted == expected) { "Expected intakes, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTotalCaffeineForDate returns flow from dao`() = runTest {
        coEvery { intakeDao.getTotalCaffeineByDate(any(), any()) } returns flowOf(126)

        repository.getTotalCaffeineForDate(LocalDate(2026, 1, 1)).test {
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
        val intakes = listOf(sampleEntityIntake)
        coEvery { intakeDao.getRecentIntakes(50) } returns flowOf(intakes)

        repository.getRecentIntakes(50).test {
            val emitted = awaitItem()
            val expected = intakes.map { it.toCommon() }
            assert(emitted == expected) { "Expected intakes, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Drink tests ---

    @Test
    fun `getAllDrinks delegates to dao`() = runTest {
        val drinks = listOf(sampleEntityDrink)
        coEvery { drinkDao.getAllDrinks() } returns flowOf(drinks)

        repository.getAllDrinks().test {
            val emitted = awaitItem()
            val expected = drinks.map { it.toCommon() }
            assert(emitted == expected) { "Expected drinks, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getDrinkById delegates to dao`() = runTest {
        coEvery { drinkDao.getDrinkById(1L) } returns sampleEntityDrink

        val result = repository.getDrinkById(1L)

        assert(result == sampleEntityDrink.toCommon()) { "Expected drink, got $result" }
        coVerify { drinkDao.getDrinkById(1L) }
    }

    @Test
    fun `addDrink delegates to dao and returns id`() = runTest {
        coEvery { drinkDao.insert(any()) } returns 7L

        val commonDrink = sampleEntityDrink.toCommon()
        val result = repository.addDrink(commonDrink)

        assert(result == 7L) { "Expected 7L, got $result" }
        coVerify { drinkDao.insert(match { it.name == "Espresso" && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `updateDrink delegates to dao`() = runTest {
        coEvery { drinkDao.update(any()) } returns Unit

        val commonDrink = sampleEntityDrink.toCommon()
        repository.updateDrink(commonDrink)

        coVerify { drinkDao.update(match { it.id == sampleEntityDrink.id && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `deleteDrink delegates to dao`() = runTest {
        coEvery { drinkDao.delete(any()) } returns Unit

        val commonDrink = sampleEntityDrink.toCommon()
        repository.deleteDrink(commonDrink)

        coVerify { drinkDao.delete(match { it.id == sampleEntityDrink.id && it.sourceDeviceId == "phone" }) }
    }

    @Test
    fun `searchDrinks delegates to dao`() = runTest {
        val drinks = listOf(sampleEntityDrink)
        coEvery { drinkDao.searchDrinks("Espresso") } returns flowOf(drinks)

        repository.searchDrinks("Espresso").test {
            val emitted = awaitItem()
            val expected = drinks.map { it.toCommon() }
            assert(emitted == expected) { "Expected drinks, got $emitted" }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
