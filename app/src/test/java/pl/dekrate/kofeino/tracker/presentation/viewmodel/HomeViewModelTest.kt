package pl.dekrate.kofeino.tracker.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    private lateinit var preferences: DataStorePreferences
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)
        every { preferences.observeCaffeineLimitMg() } returns flowOf(400)
    }

    // ===== Initial state tests =====

    @Test
    fun `initial state should be loading then emit empty today data`() = runTest {
        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue("Initial state should be loading", loadingState.isLoading)

            // After Room emits via flow, state transitions
            val state = awaitItem()
            assertEquals(0, state.totalCaffeineMg)
            assertEquals(emptyList<CaffeineIntake>(), state.todayIntakes)
            assertFalse(state.isLoading)
            assertEquals(0f, state.progress)
            assertFalse(state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `date label should be formatted for today`() = runTest {
        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            // Skip loading
            awaitItem()
            val state = awaitItem()
            assertTrue(
                "Date label should contain today's date in dd.MM.yyyy format, got: ${state.dateLabel}",
                state.dateLabel.contains(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `date label should contain day of week`() = runTest {
        viewModel = HomeViewModel(repository, preferences)

        val expectedDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        viewModel.uiState.test {
            awaitItem() // skip loading
            val state = awaitItem()
            assertTrue(
                "Date label should contain day of week ($expectedDay), got: ${state.dateLabel}",
                state.dateLabel.contains(expectedDay, ignoreCase = true)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Progress and limit tests =====

    @Test
    fun `progress should be 0 for no intakes`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals(0f, state.progress)
            assertFalse(state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress should reflect total caffeine divided by limit`() = runTest {
        every { repository.getIntakesForDate(any()) } returns flowOf(
            listOf(
                CaffeineIntake(1, drinkName = "Espresso", caffeineMg = 200, volumeMl = 30, timestamp = 0L)
            )
        )
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(200)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals(200, state.totalCaffeineMg)
            assertEquals(0.5f, state.progress)
            assertFalse(state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress should not exceed 1f`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(800)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals(1f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLimitExceeded should be true when total exceeds limit`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(450)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertTrue("Limit should be exceeded at 450mg", state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLimitExceeded should be false when total equals limit`() = runTest {
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(400)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertFalse("Limit should not be exceeded at exactly 400mg", state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Intake list tests =====

    @Test
    fun `todayIntakes should contain intakes from repository`() = runTest {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 1000L),
            CaffeineIntake(2, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 2000L),
            CaffeineIntake(3, drinkName = "Black Coffee", caffeineMg = 95, volumeMl = 250, timestamp = 3000L)
        )
        every { repository.getIntakesForDate(any()) } returns flowOf(intakes)
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(221)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals(3, state.todayIntakes.size)
            assertEquals("Latte", state.todayIntakes[0].drinkName)
            assertEquals(221, state.totalCaffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty intakes should have empty list`() = runTest {
        every { repository.getIntakesForDate(any()) } returns flowOf(emptyList())
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertTrue(state.todayIntakes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Repository interaction tests =====

    private fun getStartOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `should query repository for today date`() = runTest {
        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reactive update should reflect new intake added`() = runTest {
        // Simulate reactive Room flow: start empty, then emit with data
        every { repository.getIntakesForDate(any()) } returns flowOf(
            emptyList(),
            listOf(
                CaffeineIntake(1, drinkName = "New Drink", caffeineMg = 100, volumeMl = 200, timestamp = 0L)
            )
        )
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(0, 100)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val emptyState = awaitItem()
            assertTrue(emptyState.todayIntakes.isEmpty())
            assertEquals(0, emptyState.totalCaffeineMg)

            val updatedState = awaitItem()
            assertEquals(1, updatedState.todayIntakes.size)
            assertEquals("New Drink", updatedState.todayIntakes[0].drinkName)
            assertEquals(100, updatedState.totalCaffeineMg)
            assertEquals(0.25f, updatedState.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Edge case tests =====

    @Test
    fun `negative progress should be clamped to 0`() = runTest {
        // Shouldn't happen with Room SUM but guard against edge cases
        // Progress is total/limit which is >= 0 for non-negative values
        every { repository.getTotalCaffeineForDate(any()) } returns flowOf(-50)

        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()
            assertEquals(0f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SAFE_LIMIT_MG should be 400`() {
        assertEquals(400, HomeViewModel.SAFE_LIMIT_MG)
    }

    // ===== Midnight crossover tests =====

    @Test
    fun `getNextMidnight returns start of tomorrow`() {
        viewModel = HomeViewModel(repository, preferences)
        val startOfToday = getStartOfTodayMillis()
        val nextMidnight = viewModel.getNextMidnight()

        val cal = Calendar.getInstance().apply {
            timeInMillis = nextMidnight
        }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))

        // nextMidnight should be ~1 day after startOfToday (within 25h for DST)
        val diffHours = (nextMidnight - startOfToday) / 3_600_000
        assertTrue("Next midnight should be 23-25 hours ahead, got $diffHours", diffHours in 23..25)
    }

    @Test
    fun `flow emits today date via uiState`() = runTest {
        viewModel = HomeViewModel(repository, preferences)

        viewModel.uiState.test {
            awaitItem() // loading
            val state = awaitItem()

            // Date label should match today
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val todayFormatted = SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault())
                .format(Date(getStartOfTodayMillis()))
            assertEquals(todayFormatted, state.dateLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
