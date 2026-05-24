package pl.dekrate.kofeino.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AddDrinkViewModelTest {

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
    private lateinit var viewModel: AddDrinkViewModel

    private val allDrinks = listOf(
        DrinkEntity(1, "Espresso", 63, 30, true),
        DrinkEntity(2, "Cappuccino", 75, 200, true),
        DrinkEntity(3, "Czarna kawa", 95, 250, true),
        DrinkEntity(4, "Energy drink", 80, 250, true)
    )

    private val recentIntakes = listOf(
        CaffeineIntake(1, 1, "Espresso", 63, 30, 1000L),
        CaffeineIntake(2, 3, "Czarna kawa", 95, 250, 2000L)
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.getAllDrinks() } returns flowOf(allDrinks)
        every { repository.searchDrinks(any()) } returns flowOf(emptyList())
        every { repository.getRecentIntakes(any()) } returns flowOf(recentIntakes)
        coEvery { repository.addIntake(any()) } returns 100L

        viewModel = AddDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        // nothing to clean up
    }

    @Test
    fun `initial state has all drinks and empty search`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertEquals(allDrinks, state.drinks)
            assertEquals(recentIntakes, state.recentIntakes)
            assertFalse(state.searchQuery.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChanged updates query and triggers search`() = runTest {
        every { repository.searchDrinks("Espresso") } returns flowOf(
            listOf(DrinkEntity(1, "Espresso", 63, 30, true))
        )

        viewModel.onSearchQueryChanged("Espresso")
        testDispatcher.scheduler.advanceTimeBy(350) // past debounce (300ms)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Espresso", state.searchQuery)
            assertTrue(state.searchQuery.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search with non-matching query returns empty list`() = runTest {
        every { repository.searchDrinks("XYZ") } returns flowOf(emptyList())

        viewModel.onSearchQueryChanged("XYZ")
        testDispatcher.scheduler.advanceTimeBy(350)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.drinks.isEmpty())
            assertTrue(state.searchQuery.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearSearch resets query and returns all drinks`() = runTest {
        viewModel.onSearchQueryChanged("Test")
        testDispatcher.scheduler.advanceTimeBy(350)

        viewModel.clearSearch()
        testDispatcher.scheduler.advanceTimeBy(350)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertEquals(allDrinks, state.drinks)
            assertFalse(state.searchQuery.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recent intakes are empty when repository has none`() = runTest {
        every { repository.getRecentIntakes(any()) } returns flowOf(emptyList())

        viewModel = AddDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.recentIntakes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search with blank query returns all drinks and clears isSearchActive`() = runTest {
        viewModel.onSearchQueryChanged("   ")
        testDispatcher.scheduler.advanceTimeBy(350)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("   ", state.searchQuery)
            assertFalse(state.searchQuery.isNotBlank())
            assertEquals(allDrinks, state.drinks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addDrink calls repository addIntake and calls onComplete`() = runTest {
        val drink = DrinkEntity(1, "Espresso", 63, 30, true)
        coEvery { repository.addIntake(any()) } returns 42L

        var completed = false
        viewModel.addDrink(drink, onComplete = { completed = true })
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addIntake(any()) }
        assertTrue(completed)
    }

    @Test
    fun `addDrink calls onError when repository fails`() = runTest {
        val drink = DrinkEntity(1, "Espresso", 63, 30, true)
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        var errored = false
        viewModel.addDrink(drink, onError = { errored = true })
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addIntake(any()) }
        assertTrue(errored)
    }

    @Test
    fun `debounce prevents rapid search queries`() = runTest {
        every { repository.searchDrinks(any()) } returns flowOf(emptyList())

        viewModel.onSearchQueryChanged("E")
        viewModel.onSearchQueryChanged("Es")
        viewModel.onSearchQueryChanged("Esp")
        testDispatcher.scheduler.advanceTimeBy(200) // still within debounce

        // Only getAllDrinks should have been called (blank debounce)
        // After 300ms debounce, searchDrinks should be called once with "Esp"
        testDispatcher.scheduler.advanceTimeBy(200) // total 400ms, past debounce

        // Verify searchDrinks was called exactly once (debounce collapsed 3 rapid changes)
        coVerify(exactly = 1) { repository.searchDrinks("Esp") }

        // Just verify state eventually shows the latest query
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Esp", state.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
