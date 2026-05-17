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
import kotlinx.coroutines.flow.MutableStateFlow
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
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

@OptIn(ExperimentalCoroutinesApi::class)
class ManageDrinksViewModelTest {

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
    private lateinit var viewModel: ManageDrinksViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    // ===== Initial state =====

    @Test
    fun `uiState should load drinks from repository on init`() = runTest {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, isDefault = true),
            DrinkEntity(2, "My Drink", 100, 200, isDefault = false)
        )
        every { repository.getAllDrinks() } returns flowOf(drinks)

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.drinks.size)
        assertEquals("Espresso", state.drinks[0].name)
        assertEquals("My Drink", state.drinks[1].name)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `uiState should have empty drinks list when repository returns empty`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.drinks.isEmpty())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    // ===== Delete operations =====

    @Test
    fun `requestDelete on non-default drink should show delete confirmation`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Custom", 50, 100, isDefault = false)
        viewModel.requestDelete(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Delete confirmation should be set", viewModel.uiState.value.deleteConfirmation)
        assertEquals(drink.id, viewModel.uiState.value.deleteConfirmation?.id)
    }

    @Test
    fun `requestDelete on default drink should set error and not show confirmation`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val defaultDrink = DrinkEntity(1, "Espresso", 63, 30, isDefault = true)
        viewModel.requestDelete(defaultDrink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Delete confirmation should be null for default drinks",
            viewModel.uiState.value.deleteConfirmation)
        assertEquals(ManageDrinksError.DefaultDrinkNotDeletable, viewModel.uiState.value.error)
    }

    @Test
    fun `cancelDelete should clear delete confirmation`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Custom", 50, 100, isDefault = false)
        viewModel.requestDelete(drink)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cancelDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Delete confirmation should be cleared", viewModel.uiState.value.deleteConfirmation)
    }

    @Test
    fun `confirmDelete should call repository deleteDrink on non-default drink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.deleteDrink(any()) } returns Unit

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Custom", 50, 100, isDefault = false)
        viewModel.requestDelete(drink)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteDrink(drink) }
        assertNull("Delete confirmation should be cleared after confirm",
            viewModel.uiState.value.deleteConfirmation)
    }

    @Test
    fun `confirmDelete on default drink should set error and not call repository`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val defaultDrink = DrinkEntity(1, "Espresso", 63, 30, isDefault = true)
        viewModel.requestDelete(defaultDrink) // this sets error, null confirmation
        testDispatcher.scheduler.advanceUntilIdle()

        // confirmDelete should be no-op since no confirmation set
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteDrink(any()) }
    }

    @Test
    fun `confirmDelete should set error when repository throws`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.deleteDrink(any()) } throws RuntimeException("DB error")

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Custom", 50, 100, isDefault = false)
        viewModel.requestDelete(drink)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ManageDrinksError.DeleteFailed, viewModel.uiState.value.error)
        assertNull("Delete confirmation should be cleared on error",
            viewModel.uiState.value.deleteConfirmation)
    }

    // ===== Add / Edit form operations =====

    @Test
    fun `showAddForm should set showAddForm to true and clear editDrink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showAddForm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAddForm)
        assertNull(viewModel.uiState.value.editDrink)
    }

    @Test
    fun `showEditForm should set showAddForm and editDrink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(1, "Custom", 50, 100, isDefault = false)
        viewModel.showEditForm(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAddForm)
        assertEquals(drink.id, viewModel.uiState.value.editDrink?.id)
    }

    @Test
    fun `dismissForm should clear showAddForm and editDrink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showAddForm()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissForm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showAddForm)
        assertNull(viewModel.uiState.value.editDrink)
    }

    @Test
    fun `saveDrink should call addDrink when no editDrink is set`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.addDrink(any()) } returns 1L

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveDrink("Flat White", 130, 200)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addDrink(any()) }
        assertEquals(false, viewModel.uiState.value.showAddForm)
    }

    @Test
    fun `saveDrink should pass correct values to addDrink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        val drinkSlot = slot<DrinkEntity>()
        coEvery { repository.addDrink(capture(drinkSlot)) } returns 1L

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveDrink("Flat White", 130, 200)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Flat White", drinkSlot.captured.name)
        assertEquals(130, drinkSlot.captured.caffeineMg)
        assertEquals(200, drinkSlot.captured.volumeMl)
        assertEquals(false, drinkSlot.captured.isDefault)
    }

    @Test
    fun `saveDrink should call updateDrink when editDrink is set`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        coEvery { repository.updateDrink(any()) } returns Unit

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 5, name = "Old Name", caffeineMg = 50, volumeMl = 100)
        viewModel.showEditForm(drink)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.saveDrink("New Name", 60, 150)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateDrink(any()) }
        assertEquals(false, viewModel.uiState.value.showAddForm)
        assertNull(viewModel.uiState.value.editDrink)
    }

    @Test
    fun `saveDrink should pass correct updated values to updateDrink`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        val drinkSlot = slot<DrinkEntity>()
        coEvery { repository.updateDrink(capture(drinkSlot)) } returns Unit

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 5, name = "Old Name", caffeineMg = 50, volumeMl = 100)
        viewModel.showEditForm(drink)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.saveDrink("New Name", 60, 150)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5L, drinkSlot.captured.id)
        assertEquals("New Name", drinkSlot.captured.name)
        assertEquals(60, drinkSlot.captured.caffeineMg)
        assertEquals(150, drinkSlot.captured.volumeMl)
    }

    // ===== Error handling =====

    @Test
    fun `clearError should set error to null`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger an error
        viewModel.requestDelete(
            DrinkEntity(1, "Espresso", 63, 30, isDefault = true)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent when error is null`() = runTest {
        every { repository.getAllDrinks() } returns flowOf(emptyList())

        viewModel = ManageDrinksViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }
}
