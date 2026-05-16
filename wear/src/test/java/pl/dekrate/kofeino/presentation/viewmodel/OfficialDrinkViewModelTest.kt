package pl.dekrate.kofeino.presentation.viewmodel

import app.cash.turbine.test
import pl.dekrate.kofeino.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.domain.model.OfficialDrink
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class OfficialDrinkViewModelTest {

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

    private lateinit var repository: OfficialDrinkRepository
    private lateinit var viewModel: OfficialDrinkViewModel

    private val sampleDrinks = listOf(
        OfficialDrink("a", "Kawa", "BrandA", 63.0, null, "250ml"),
        OfficialDrink("b", "Herbata", "BrandB", 28.0, 1.0, "200ml"),
        OfficialDrink("c", "Cola", "BrandC", 10.0, 42.0, "330ml")
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        coEvery { repository.getOfficialDrinks() } returns Result.success(sampleDrinks)
        coEvery { repository.searchOfficialDrinks(any()) } returns Result.success(emptyList())
    }

    @Test
    fun `initial state should load and show drinks`() = runTest {
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.drinks.size)
            assertEquals(false, state.isLoading)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadOfficialDrinks should set error on failure`() = runTest {
        coEvery { repository.getOfficialDrinks() } returns Result.failure(Exception("No network"))

        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.drinks.size)
            assertEquals(false, state.isLoading)
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh should call repository again`() = runTest {
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 2) { repository.getOfficialDrinks() }
    }

    @Test
    fun `state should emit final loaded state`() = runTest {
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isLoading)
            assertEquals(3, state.drinks.size)
            assertEquals("Kawa", state.drinks[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Search tests =====

    @Test
    fun `onSearchQueryChanged should enable search mode`() = runTest {
        coEvery { repository.searchOfficialDrinks("Kawa") } returns Result.success(sampleDrinks.take(1))
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Kawa")
        testDispatcher.scheduler.advanceUntilIdle()
        // Debounce 500ms — musimy odczekać
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Kawa", state.searchQuery)
            assertEquals(true, state.isSearchMode)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.searchOfficialDrinks("Kawa") }
    }

    @Test
    fun `clearSearch should reset to initial browse mode`() = runTest {
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Cola")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertEquals(false, state.isSearchMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh in search mode should call searchOfficialDrinks`() = runTest {
        coEvery { repository.searchOfficialDrinks(any()) } returns Result.success(sampleDrinks)
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Herbata")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        // Refresh w trybie wyszukiwania
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 2) { repository.searchOfficialDrinks("Herbata") }
    }

    @Test
    fun `empty search query should reload official drinks`() = runTest {
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        // Po pustym zapytaniu powinien załadować wszystkie napoje
        coVerify(atLeast = 2) { repository.getOfficialDrinks() }
    }

    @Test
    fun `search should show error on failure`() = runTest {
        coEvery { repository.searchOfficialDrinks(any()) } returns
            Result.failure(Exception("Not found"))
        viewModel = OfficialDrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.search("xyz")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            // drinks nie są czyszczone przy błędzie — poprzednie wyniki pozostają
            assertEquals(3, state.drinks.size)
            assertEquals(false, state.isLoading)
            assertNotNull(state.error)
            assertEquals("Not found", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
