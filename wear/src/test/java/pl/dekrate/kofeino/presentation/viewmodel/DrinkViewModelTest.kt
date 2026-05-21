package pl.dekrate.kofeino.presentation.viewmodel

import app.cash.turbine.test
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.domain.model.DrinkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

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
        // Default: empty drinks list
        every { repository.getAllDrinks() } returns flowOf(emptyList())
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should emit empty drinks list`() = runTest {
        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.allDrinks.test {
            val drinks = awaitItem()
            assertTrue("Initial drinks list should be empty", drinks.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state should observe repository getAllDrinks flow`() = runTest {
        val drinks = listOf(
            DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        )
        every { repository.getAllDrinks() } returns flowOf(drinks)

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.allDrinks.test {
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals("Espresso", emitted[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== addDrink tests =====

    @Test
    fun `addDrink should call repository addDrink with correct values`() = runTest {
        coEvery { repository.addDrink(any()) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addDrink("Custom Brew", 85, 300)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addDrink(match {
                it.name == "Custom Brew" &&
                    it.caffeineMg == 85 &&
                    it.volumeMl == 300 &&
                    !it.isDefault
            })
        }
    }

    @Test
    fun `addDrink with minimum values should succeed`() = runTest {
        coEvery { repository.addDrink(any()) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addDrink("Mini", 1, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addDrink(match {
                it.name == "Mini" && it.caffeineMg == 1 && it.volumeMl == 1
            })
        }
    }

    @Test
    fun `addDrink with special characters in name should work`() = runTest {
        coEvery { repository.addDrink(any()) } returns 1L

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addDrink("Kawa & Herbata (1/2)", 50, 200)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addDrink(match {
                it.name == "Kawa & Herbata (1/2)"
            })
        }
    }

    // ===== updateDrink tests =====

    @Test
    fun `updateDrink should call repository updateDrink`() = runTest {
        coEvery { repository.updateDrink(any()) } just Runs

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        viewModel.updateDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateDrink(drink) }
    }

    @Test
    fun `updateDrink with name change should propagate`() = runTest {
        coEvery { repository.updateDrink(any()) } just Runs

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val original = DrinkEntity(id = 1, name = "Old Name", caffeineMg = 50, volumeMl = 200)
        val updated = original.copy(name = "New Name", caffeineMg = 75)
        viewModel.updateDrink(updated)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateDrink(updated) }
    }

    @Test
    fun `updateDrink with isDefault flag should not be stripped`() = runTest {
        coEvery { repository.updateDrink(any()) } just Runs

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val defaultDrink = DrinkEntity(id = 5, name = "Default Coffee", caffeineMg = 95, volumeMl = 250, isDefault = true)
        viewModel.updateDrink(defaultDrink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateDrink(defaultDrink) }
    }

    // ===== deleteDrink tests =====

    @Test
    fun `deleteDrink should call repository deleteDrink`() = runTest {
        coEvery { repository.deleteDrink(any()) } just Runs

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 3, name = "Delete Me", caffeineMg = 50, volumeMl = 200)
        viewModel.deleteDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteDrink(drink) }
    }

    @Test
    fun `deleteDrink should work with newly created drink`() = runTest {
        coEvery { repository.addDrink(any()) } returns 10L
        coEvery { repository.deleteDrink(any()) } just Runs

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addDrink("Temp", 30, 150)
        testDispatcher.scheduler.advanceUntilIdle()

        val deleteMe = DrinkEntity(id = 10, name = "Temp", caffeineMg = 30, volumeMl = 150)
        viewModel.deleteDrink(deleteMe)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteDrink(deleteMe) }
    }

    // ===== Flow reactivity tests =====

    @Test
    fun `viewModel should reflect repository drink list changes`() = runTest {
        val drinksFlow = MutableStateFlow(
            listOf(DrinkEntity(id = 1, name = "A", caffeineMg = 10, volumeMl = 100))
        )
        every { repository.getAllDrinks() } returns drinksFlow

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial emission from the ViewModel's allDrinks StateFlow
        viewModel.allDrinks.test {
            val initial = awaitItem()
            assertEquals(1, initial.size)
            assertEquals("A", initial[0].name)

            // Simulate repository adding a drink
            drinksFlow.value = listOf(
                DrinkEntity(id = 1, name = "A", caffeineMg = 10, volumeMl = 100),
                DrinkEntity(id = 2, name = "B", caffeineMg = 20, volumeMl = 200)
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(2, updated.size)
            assertEquals("B", updated[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel should emit empty when repository has no drinks`() = runTest {
        val drinksFlow = MutableStateFlow(
            listOf(DrinkEntity(id = 1, name = "Only", caffeineMg = 10, volumeMl = 100))
        )
        every { repository.getAllDrinks() } returns drinksFlow

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.allDrinks.test {
            assertEquals(1, awaitItem().size)

            // Simulate deleting all drinks
            drinksFlow.value = emptyList()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewModel should handle rapid repository emissions`() = runTest {
        val drinksFlow = MutableStateFlow(emptyList<DrinkEntity>())
        every { repository.getAllDrinks() } returns drinksFlow

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Rapid updates
        drinksFlow.value = listOf(DrinkEntity(id = 1, name = "A", caffeineMg = 10, volumeMl = 100))
        drinksFlow.value = listOf(
            DrinkEntity(id = 1, name = "A", caffeineMg = 10, volumeMl = 100),
            DrinkEntity(id = 2, name = "B", caffeineMg = 20, volumeMl = 200)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.allDrinks.test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Drinks ordered by name (as per DAO query) =====

    @Test
    fun `viewModel should preserve drink order from repository`() = runTest {
        val drinks = listOf(
            DrinkEntity(id = 3, name = "C", caffeineMg = 30, volumeMl = 300),
            DrinkEntity(id = 1, name = "A", caffeineMg = 10, volumeMl = 100),
            DrinkEntity(id = 2, name = "B", caffeineMg = 20, volumeMl = 200)
        )
        every { repository.getAllDrinks() } returns flowOf(drinks)

        viewModel = DrinkViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.allDrinks.test {
            val list = awaitItem()
            assertEquals(3, list.size)
            // ViewModel preserves the order emitted by the repository flow
            assertEquals("C", list[0].name)
            assertEquals("A", list[1].name)
            assertEquals("B", list[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
