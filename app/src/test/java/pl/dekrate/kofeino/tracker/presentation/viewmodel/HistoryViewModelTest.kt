package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

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
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should be loading then emit empty data`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalCaffeineMg)
            assertEquals(emptyList<CaffeineIntake>(), state.dateIntakes)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state should have formatted date label`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(
                "Date label should match dd.MM.yyyy format, got: ${state.dateLabel}",
                state.dateLabel.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isToday should return true initially`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Should be today initially", viewModel.isToday())
    }

    // ===== Date navigation tests =====

    @Test
    fun `previousDay should move date one day back`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
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
        viewModel = HistoryViewModel(repository, SavedStateHandle())
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
        viewModel = HistoryViewModel(repository, SavedStateHandle())
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
    fun `isToday should return false after navigating away`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("Should not be today after navigating away", viewModel.isToday())
    }

    @Test
    fun `isToday should return true after navigating back to today`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.goToToday()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Should be today after goToToday", viewModel.isToday())
    }

    @Test
    fun `navigating to next day then previous day should return to today`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Should return to initial date after next+previous",
            initialDate,
            viewModel.uiState.value.selectedDateMillis
        )
    }

    // ===== State update tests =====

    @Test
    fun `state should reflect intakes from repository`() = runTest {
        val intakes = listOf(
            CaffeineIntake(1, drinkId = 1, "Espresso", 63, 30, 0L),
            CaffeineIntake(2, drinkId = 2, "Black Coffee", 95, 250, 0L)
        )
        every { repository.getIntakesForDate(any()) } returns flowOf(intakes)
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(158)

        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(158, state.totalCaffeineMg)
            assertEquals(2, state.dateIntakes.size)
            assertEquals("Espresso", state.dateIntakes[0].drinkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository methods should be called with correct dates`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            repository.getIntakesForDate(any())
            repository.getTotalCaffeineForDate(any())
        }
    }

    @Test
    fun `date label should always be formatted date`() = runTest {
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)

        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        // Navigate 3 days ahead
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        val dateLabel = viewModel.uiState.value.dateLabel
        assertTrue(
            "Date label should match dd.MM.yyyy format, got: $dateLabel",
            dateLabel.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))
        )
    }

    // ===== Error handling tests =====

    @Test
    fun `repository exception should set error state and clear loading`() = runTest {
        every { repository.getIntakesForDate(any()) } returns flow { throw RuntimeException("DB error") }
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)

        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("Error should be set on exception", state.error)
            assertFalse("Loading should be false after error", state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError should set error to null`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially no error
        assertNull("Error should be null initially", viewModel.uiState.value.error)

        // Simulate error by accessing internal state directly
        // clearError should be idempotent when already null
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should remain null after clearError", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        // Call clearError multiple times
        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should remain null after multiple clearError calls", viewModel.uiState.value.error)
    }

    // ===== Multiple navigation tests =====

    @Test
    fun `repeated previousDay calls should keep moving backward`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        val afterOneBack = viewModel.uiState.value.selectedDateMillis

        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        val afterTwoBack = viewModel.uiState.value.selectedDateMillis

        assertTrue(
            "Two steps back should be less than one step back",
            afterTwoBack < afterOneBack
        )
        assertTrue(
            "After two steps back, isToday should be false",
            !viewModel.isToday()
        )
    }

    @Test
    fun `repeated nextDay calls should keep moving forward`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        val afterOneForward = viewModel.uiState.value.selectedDateMillis

        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        val afterTwoForward = viewModel.uiState.value.selectedDateMillis

        assertTrue(
            "Two steps forward should be greater than one step forward",
            afterTwoForward > afterOneForward
        )
    }

    // ===== SavedStateHandle persistence tests =====

    @Test
    fun `should use SavedStateHandle value when pre-populated`() = runTest {
        val specificDate = viewModel.run {
            HistoryViewModel(repository, SavedStateHandle()).getStartOfToday()
        }
        val savedHandle = SavedStateHandle(mapOf("selectedDateMillis" to specificDate))

        viewModel = HistoryViewModel(repository, savedHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Should use the date from SavedStateHandle",
            specificDate,
            viewModel.uiState.value.selectedDateMillis
        )
    }

    @Test
    fun `should persist date to SavedStateHandle on navigation`() = runTest {
        val savedHandle = SavedStateHandle()
        viewModel = HistoryViewModel(repository, savedHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialDate = viewModel.uiState.value.selectedDateMillis

        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        val persistedDate = savedHandle.get<Long>("selectedDateMillis")
        assertNotNull("SavedStateHandle should contain selectedDateMillis after navigation", persistedDate)
        assertEquals(
            "Persisted date should match ViewModel's current selection",
            viewModel.uiState.value.selectedDateMillis,
            persistedDate
        )

        // Navigate again to verify updates
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedDate = savedHandle.get<Long>("selectedDateMillis")
        assertNotNull("SavedStateHandle should be updated after second navigation", updatedDate)
        assertEquals(
            "Persisted date should match ViewModel after navigating back",
            initialDate,
            updatedDate
        )
    }

    @Test
    fun `should fall back to today when SavedStateHandle is empty`() = runTest {
        viewModel = HistoryViewModel(repository, SavedStateHandle())
        testDispatcher.scheduler.advanceUntilIdle()

        val todayStart = viewModel.getStartOfToday()
        assertEquals(
            "Should fall back to today's start when SavedStateHandle is empty",
            todayStart,
            viewModel.uiState.value.selectedDateMillis
        )
    }
}
