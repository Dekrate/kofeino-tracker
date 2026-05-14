package pl.dekrate.kofeino.presentation.viewmodel

import app.cash.turbine.test
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CaffeineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = object : TestWatcher() {
        override fun starting(description: Description?) {
            Dispatchers.setMain(testDispatcher)
        }
        override fun finished(description: Description?) {
            Dispatchers.resetMain()
        }
    }

    private lateinit var repository: CaffeineRepository
    private lateinit var viewModel: CaffeineViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        // Default mocks for flows used in init
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)
        every { repository.getAllDrinks() } returns flowOf(emptyList())
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should be empty with zero total`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalCaffeineMg)
            assertEquals(emptyList<CaffeineIntake>(), state.dateIntakes)
            assertFalse(state.isLimitExceeded)
            assertEquals(0f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state should have today date label`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Dzisiaj", state.dateLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Drink add tests =====

    @Test
    fun `adding drink should call repository addIntake`() = runTest {
        coEvery { repository.addIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        viewModel.addDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addIntake(match {
                it.drinkId == 1L && it.drinkName == "Espresso" && it.caffeineMg == 63
            })
        }
    }

    @Test
    fun `adding drink with DrinkEntity should set correct fields`() = runTest {
        coEvery { repository.addIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 5, name = "Latte", caffeineMg = 63, volumeMl = 250)
        viewModel.addDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addIntake(match {
                it.drinkId == 5L && it.drinkName == "Latte" &&
                    it.caffeineMg == 63 && it.volumeMl == 250
            })
        }
    }

    // ===== State update tests =====

    @Test
    fun `state should reflect total caffeine from repository`() = runTest {
        val intakes = listOf(
            CaffeineIntake(1, drinkId = 1, "Espresso", 63, 30, 0L),
            CaffeineIntake(2, drinkId = 2, "Black Coffee", 95, 250, 0L)
        )
        every { repository.getIntakesForDate(any()) } returns flowOf(intakes)
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(158)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(158, state.totalCaffeineMg)
            assertEquals(2, state.dateIntakes.size)
            assertEquals(0.395f, state.progress, 0.001f)
            assertFalse(state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `limit exceeded should be true when total over 400mg`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(450)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isLimitExceeded)
            assertEquals(1f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress should be capped at 1 when over limit`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(800)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Intake CRUD tests =====

    @Test
    fun `updateIntake should call repository updateIntake`() = runTest {
        coEvery { repository.updateIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        viewModel.updateIntake(intake)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateIntake(intake) }
    }

    @Test
    fun `deleteIntake should call repository deleteIntake`() = runTest {
        coEvery { repository.deleteIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        viewModel.deleteIntake(intake)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteIntake(intake) }
    }

    // ===== Date navigation tests =====

    @Test
    fun `previousDay should change date label from Dzisiaj to Wczoraj`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Wczoraj", state.dateLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nextDay should change date label to Jutro`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Jutro", state.dateLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `goToToday should reset date to Dzisiaj`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.goToToday()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Dzisiaj", state.dateLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isToday should return true when on today`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isToday())
    }

    @Test
    fun `isToday should return false after navigating away`() = runTest {
        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isToday())
    }

    // ===== Drinks in state tests =====

    @Test
    fun `state should contain drinks from repository`() = runTest {
        val drinks = listOf(
            DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30, isDefault = true),
            DrinkEntity(id = 2, name = "Latte", caffeineMg = 63, volumeMl = 250, isDefault = true)
        )
        every { repository.getAllDrinks() } returns flowOf(drinks)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.drinks.size)
            assertEquals("Espresso", state.drinks[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Combination test =====

    @Test
    fun `state should react to repository emission changes`() = runTest {
        val intakesFlow = MutableStateFlow(
            listOf(CaffeineIntake(1, drinkId = 1, "Espresso", 63, 30, 0L))
        )
        val totalFlow = MutableStateFlow(63)

        every { repository.getIntakesForDate(any()) } returns intakesFlow
        every { repository.getTotalCaffeineForDate(any()) } returns totalFlow

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(63, awaitItem().totalCaffeineMg)

            intakesFlow.value = listOf(
                CaffeineIntake(1, drinkId = 1, "Espresso", 63, 30, 0L),
                CaffeineIntake(2, drinkId = 2, "Latte", 63, 250, 0L)
            )
            totalFlow.value = 126
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(126, state.totalCaffeineMg)
            assertEquals(2, state.dateIntakes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
