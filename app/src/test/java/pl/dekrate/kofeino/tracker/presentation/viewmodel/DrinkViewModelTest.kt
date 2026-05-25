package pl.dekrate.kofeino.tracker.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity

@OptIn(ExperimentalCoroutinesApi::class)
class DrinkViewModelTest {

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
    private lateinit var viewModel: DrinkViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    // ===== Initial state tests =====

    @Test
    fun `allDrinks should be populated from repository`() = runTest {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, isDefault = true),
            DrinkEntity(2, "Latte", 63, 250, isDefault = true)
        )
        every { repository.getAllDrinks() } returns flowOf(drinks)

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.allDrinks.value.size)
        assertEquals("Espresso", viewModel.allDrinks.value[0].name)
    }

    @Test
    fun `allDrinks should be empty initially when repository returns empty`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.allDrinks.value.isEmpty())
    }

    @Test
    fun `uiState should have no error initially`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull("Error should be null initially", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== logDrink tests =====

    @Test
    fun `logDrink should create intake and call repository addIntake`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addIntake(any()) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Espresso", 63, 30)
        viewModel.logDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addIntake(any()) }
    }

    @Test
    fun `logDrink should pass correct intake values to repository`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        val intakeSlot = slot<CaffeineIntake>()
        coEvery { repository.addIntake(capture(intakeSlot)) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 42, name = "Black Coffee", caffeineMg = 95, volumeMl = 250)
        viewModel.logDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(42L, intakeSlot.captured.drinkId)
        assertEquals("Black Coffee", intakeSlot.captured.drinkName)
        assertEquals(95, intakeSlot.captured.caffeineMg)
        assertEquals(250, intakeSlot.captured.volumeMl)
        assertTrue("Timestamp should be set", intakeSlot.captured.timestamp > 0)
    }

    @Test
    fun `logDrink should call onComplete on success`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addIntake(any()) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        val drink = DrinkEntity(1, "Espresso", 63, 30)
        viewModel.logDrink(drink, onComplete = { completed = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete should be called", completed)
    }

    @Test
    fun `logDrink should set error state on repository exception`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Espresso", 63, 30)
        viewModel.logDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("Error should be set on exception", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logDrink should call onError on repository exception`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        var errorCalled = false
        val drink = DrinkEntity(1, "Espresso", 63, 30)
        viewModel.logDrink(drink, onError = { errorCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onError should be called", errorCalled)
    }

    // ===== clearError tests =====

    @Test
    fun `clearError should set error to null`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger an error
        viewModel.logDrink(DrinkEntity(1, "Espresso", 63, 30))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should be null after clearError", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent when error is null`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Call clearError multiple times with no error present
        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should remain null", viewModel.uiState.value.error)
    }
}
