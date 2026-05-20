package pl.dekrate.kofeino.presentation.viewmodel

import android.content.Context
import app.cash.turbine.test
import pl.dekrate.kofeino.data.local.CaffeinePreferences
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private lateinit var caffeinePreferences: CaffeinePreferences
    private lateinit var context: Context
    private lateinit var viewModel: CaffeineViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        caffeinePreferences = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.getString(any<Int>()) } answers {
            "string_${firstArg<Int>()}"
        }
        every { context.getString(any<Int>(), *anyVararg<Any>()) } answers {
            val resId = firstArg<Int>()
            val formatArgs = args.drop(1).joinToString(",")
            "string_${resId}_args($formatArgs)"
        }
        // Default mocks for flows used in init
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)
        every { repository.getAllDrinks() } returns flowOf(emptyList())
        // Default limit: adult 400 mg via flow
        every { caffeinePreferences.limitFlow } returns flowOf(400)
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should be empty with zero total`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("Date label should not be null", state.dateLabel)
            assertTrue("Date label should not be empty", state.dateLabel.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Drink add tests =====

    @Test
    fun `adding drink should call repository addIntake`() = runTest {
        coEvery { repository.addIntake(any()) } returns 1L

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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
        coEvery { repository.addIntake(any()) } returns 1L

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        viewModel.updateIntake(intake)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateIntake(intake) }
    }

    @Test
    fun `updateIntake onComplete should fire after successful save`() = runTest {
        coEvery { repository.updateIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackFired = false
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())

        viewModel.updateIntake(intake, onComplete = { callbackFired = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete should fire after successful update", callbackFired)
    }

    @Test
    fun `updateIntake onError should fire when save fails`() = runTest {
        coEvery { repository.updateIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        var onCompleteFired = false
        var onErrorFired = false
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())

        viewModel.updateIntake(
            intake,
            onComplete = { onCompleteFired = true },
            onError = { onErrorFired = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("onComplete should NOT fire on error", onCompleteFired)
        assertTrue("onError should fire on DB error", onErrorFired)
    }

    @Test
    fun `updateIntake onComplete should NOT fire when save fails`() = runTest {
        coEvery { repository.updateIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackFired = false
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())

        viewModel.updateIntake(intake, onComplete = { callbackFired = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("onComplete should NOT fire when update fails", callbackFired)
    }

    @Test
    fun `updateIntake should set error state on failure`() = runTest {
        coEvery { repository.updateIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        viewModel.updateIntake(intake)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteIntake should call repository deleteIntake`() = runTest {
        coEvery { repository.deleteIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        viewModel.deleteIntake(intake)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteIntake(intake) }
    }

    @Test
    fun `deleteIntake onComplete should fire after successful delete`() = runTest {
        coEvery { repository.deleteIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackFired = false
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())

        viewModel.deleteIntake(intake, onComplete = { callbackFired = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete should fire after successful delete", callbackFired)
    }

    @Test
    fun `deleteIntake onError should fire when delete fails`() = runTest {
        coEvery { repository.deleteIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        var onCompleteFired = false
        var onErrorFired = false
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())

        viewModel.deleteIntake(
            intake,
            onComplete = { onCompleteFired = true },
            onError = { onErrorFired = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("onComplete should NOT fire on delete error", onCompleteFired)
        assertTrue("onError should fire on DB error", onErrorFired)
    }

    // ===== getIntakeById tests =====

    @Test
    fun `getIntakeById should return intake from repository`() = runTest {
        val intake = CaffeineIntake(1, drinkId = 1, "Test", 50, 200, System.currentTimeMillis())
        coEvery { repository.getIntakeById(1L) } returns intake

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.getIntakeById(1L)
        assertEquals(50, result?.caffeineMg)
        assertEquals("Test", result?.drinkName)
        coVerify { repository.getIntakeById(1L) }
    }

    @Test
    fun `getIntakeById should return null for non-existent id`() = runTest {
        coEvery { repository.getIntakeById(999L) } returns null

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.getIntakeById(999L)
        assertEquals(null, result)
    }

    // ===== Date navigation tests =====

    @Test
    fun `previousDay should move date one day back`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "selectedDateMillis should be before initial date",
            viewModel.uiState.value.selectedDateMillis < initialDate
        )
    }

    @Test
    fun `nextDay should move date one day forward`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "selectedDateMillis should be after initial date",
            viewModel.uiState.value.selectedDateMillis > initialDate
        )
    }

    @Test
    fun `goToToday should reset date to start of today`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.goToToday()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Should reset to initial (today) date",
            initialDate,
            viewModel.uiState.value.selectedDateMillis
        )
    }

    @Test
    fun `isToday should return true when on today`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isToday())
    }

    @Test
    fun `isToday should return false after navigating away`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.drinks.size)
            assertEquals("Espresso", state.drinks[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== clearError tests =====

    @Test
    fun `clearError should set error to null`() = runTest {
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        viewModel.addDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error should be set after failed addDrink", viewModel.uiState.value.error)

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should be null after clearError", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent when error is null`() = runTest {
        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should be null initially", viewModel.uiState.value.error)

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should remain null after clearError on clean state", viewModel.uiState.value.error)
    }

    // ===== Combination test =====

    @Test
    fun `addDrink should set error state on failure`() = runTest {
        coEvery { repository.addIntake(any()) } throws RuntimeException("DB error")

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = DrinkEntity(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        viewModel.addDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertNotNull("Error should be set when addDrink fails", error)
    }

    // ===== Caffeine limit profile tests =====

    @Test
    fun `state should contain safeLimitMg from preferences`() = runTest {
        every { caffeinePreferences.limitFlow } returns flowOf(400)

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(400, state.safeLimitMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLimitExceeded should use 200 mg limit for pregnant profile`() = runTest {
        every { caffeinePreferences.limitFlow } returns flowOf(200)
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(250)

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(250, state.totalCaffeineMg)
            assertTrue("250 > 200 should exceed pregnant limit", state.isLimitExceeded)
            assertEquals(1f, state.progress)
            assertEquals(200, state.safeLimitMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress should use custom limit when profile is CUSTOM`() = runTest {
        every { caffeinePreferences.limitFlow } returns flowOf(150)
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(75)

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(150, state.safeLimitMg)
            assertEquals(0.5f, state.progress, 0.001f)
            assertFalse("75 < 150 should not exceed limit", state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state should react to repository emission changes`() = runTest {
        val intakesFlow = MutableStateFlow(
            listOf(CaffeineIntake(1, drinkId = 1, "Espresso", 63, 30, 0L))
        )
        val totalFlow = MutableStateFlow(63)

        every { repository.getIntakesForDate(any()) } returns intakesFlow
        every { repository.getTotalCaffeineForDate(any()) } returns totalFlow

        viewModel = CaffeineViewModel(repository, caffeinePreferences, context)
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
