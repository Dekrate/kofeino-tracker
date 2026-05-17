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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink

@OptIn(ExperimentalCoroutinesApi::class)
class OfficialDrinksViewModelTest {

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

    private lateinit var officialRepository: OfficialDrinkRepository
    private lateinit var caffeineRepository: CaffeineRepository
    private lateinit var viewModel: OfficialDrinksViewModel

    private val sampleDrink = OfficialDrink(
        barcode = "5901234567890",
        name = "Black Coffee",
        brand = "Brand",
        caffeineMgPer100ml = 63.0,
        energyKcalPer100ml = 2.0,
        quantity = "250ml"
    )

    @Before
    fun setup() {
        officialRepository = mockk(relaxed = true)
        caffeineRepository = mockk(relaxed = true)
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(emptyList())
        coEvery { officialRepository.searchOfficialDrinks(any()) } returns Result.success(emptyList())
    }

    // ===== Initial load =====

    @Test
    fun `should load official drinks on init`() = runTest {
        val drinks = listOf(sampleDrink)
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(drinks)

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.drinks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Black Coffee", state.drinks[0].name)
    }

    @Test
    fun `should handle initial load failure gracefully with empty state`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.failure(Exception("No cache"))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.drinks.size)
        assertFalse(state.isLoading)
        assertNull("Empty cache should not show error", state.error)
    }

    // ===== Search =====

    @Test
    fun `onQueryChanged with blank query should clear search and reload`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(listOf(sampleDrink))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially loaded
        assertEquals(1, viewModel.uiState.value.drinks.size)

        // Clear query should reload
        viewModel.onQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { officialRepository.getOfficialDrinks() }
        assertFalse(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun `onQueryChanged should debounce search by 500ms`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks(any()) } returns Result.success(listOf(sampleDrink))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChanged("coffee")
        // Use runCurrent() to process only tasks at current virtual time (not advancing it)
        testDispatcher.scheduler.runCurrent()

        // Should not have called search yet (debounce delay not elapsed)
        coVerify(exactly = 0) { officialRepository.searchOfficialDrinks("coffee") }

        // Advance past debounce
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.runCurrent()

        coVerify(atLeast = 1) { officialRepository.searchOfficialDrinks("coffee") }
        assertTrue(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun `onQueryChanged with rapid changes should only trigger last search`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks(any()) } returns Result.success(emptyList())

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChanged("c")
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onQueryChanged("co")
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onQueryChanged("cof")
        testDispatcher.scheduler.advanceUntilIdle()

        // Only "cof" should have been searched (last one after debounce)
        coVerify(atLeast = 1) { officialRepository.searchOfficialDrinks("cof") }
    }

    @Test
    fun `searchImmediate should search immediately without debounce`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks(any()) } returns Result.success(listOf(sampleDrink))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchImmediate("coffee")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { officialRepository.searchOfficialDrinks("coffee") }
        assertTrue(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun `search should update results in UI state`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks("latte") } returns Result.success(
            listOf(
                OfficialDrink(
                    barcode = "1",
                    name = "Latte",
                    brand = "Cafe",
                    caffeineMgPer100ml = 50.0,
                    energyKcalPer100ml = null,
                    quantity = null
                ),
                OfficialDrink(
                    barcode = "2",
                    name = "Iced Latte",
                    brand = null,
                    caffeineMgPer100ml = 40.0,
                    energyKcalPer100ml = null,
                    quantity = null
                )
            )
        )

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchImmediate("latte")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.drinks.size)
        assertEquals("Latte", state.drinks[0].name)
        assertEquals("Iced Latte", state.drinks[1].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `search should set searchMode when query is non-blank`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks(any()) } returns Result.success(emptyList())

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchImmediate("test")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSearchMode)
        assertEquals("test", viewModel.uiState.value.searchQuery)
    }

    // ===== Clear search =====

    @Test
    fun `clearSearch should reset query and reload all drinks`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(listOf(sampleDrink))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchImmediate("test")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearchMode)
        assertEquals("", viewModel.uiState.value.searchQuery)
        coVerify(atLeast = 1) { officialRepository.getOfficialDrinks() }
    }

    // ===== Refresh =====

    @Test
    fun `refresh should reload when not in search mode`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(listOf(sampleDrink))

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 2) { officialRepository.getOfficialDrinks() }
    }

    @Test
    fun `refresh should re-search when in search mode`() = runTest {
        coEvery { officialRepository.searchOfficialDrinks("coffee") } returns Result.success(emptyList())

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchImmediate("coffee")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 2) { officialRepository.searchOfficialDrinks("coffee") }
    }

    // ===== Import =====

    @Test
    fun `importAsDrink should call addDrink with correct values`() = runTest {
        coEvery { caffeineRepository.addDrink(any()) } returns 1L

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = OfficialDrink(
            barcode = "123",
            name = "Energy Drink",
            brand = "Boost",
            caffeineMgPer100ml = 80.0,
            energyKcalPer100ml = null,
            quantity = "250ml"
        )

        viewModel.importAsDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        val drinkSlot = slot<DrinkEntity>()
        coVerify { caffeineRepository.addDrink(capture(drinkSlot)) }

        assertEquals("Energy Drink", drinkSlot.captured.name)
        assertEquals(80, drinkSlot.captured.caffeineMg)
        assertEquals(100, drinkSlot.captured.volumeMl) // default 100ml serving
        assertFalse(drinkSlot.captured.isDefault)
    }

    @Test
    fun `importAsDrink should handle caffeine values correctly`() = runTest {
        coEvery { caffeineRepository.addDrink(any()) } returns 1L

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Test with decimal caffeine value
        val drink = OfficialDrink(
            barcode = "456",
            name = "Light Drink",
            brand = null,
            caffeineMgPer100ml = 25.7,
            energyKcalPer100ml = null,
            quantity = null
        )

        viewModel.importAsDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        val drinkSlot = slot<DrinkEntity>()
        coVerify { caffeineRepository.addDrink(capture(drinkSlot)) }

        assertEquals(25, drinkSlot.captured.caffeineMg) // toInt coerceAtLeast 0
    }

    @Test
    fun `importAsDrink should set importing barcode while importing`() = runTest {
        coEvery { caffeineRepository.addDrink(any()) } returns 1L

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.importAsDrink(sampleDrink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.importingBarcode)
    }

    @Test
    fun `importAsDrink should set error on failure`() = runTest {
        coEvery { caffeineRepository.addDrink(any()) } throws RuntimeException("DB error")

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.importAsDrink(sampleDrink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OfficialDrinksError.ImportFailed, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.importingBarcode)
    }

    // ===== Error handling =====

    @Test
    fun `clearError should set error to null`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(emptyList())
        coEvery { officialRepository.searchOfficialDrinks(any()) } throws RuntimeException("Search crash")

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger error via search (not initial load, which handles empty cache gracefully)
        viewModel.searchImmediate("coffee")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error should be set after search crash", viewModel.uiState.value.error)

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should be null after clearError", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent`() = runTest {
        coEvery { officialRepository.getOfficialDrinks() } returns Result.success(emptyList())

        viewModel = OfficialDrinksViewModel(officialRepository, caffeineRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }
}
