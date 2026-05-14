package com.example.kofeinotracker.presentation.viewmodel

import app.cash.turbine.test
import com.example.kofeinotracker.data.repository.CaffeineRepository
import com.example.kofeinotracker.domain.model.CaffeineDrink
import com.example.kofeinotracker.domain.model.CaffeineIntake
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    }

    @After
    fun tearDown() {
        // cleanup
    }

    @Test
    fun `initial state should be empty with zero total`() = runTest {
        every { repository.getTodayIntakes() } returns flowOf(emptyList())
        every { repository.getTodayTotalCaffeine() } returns flowOf(0)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalCaffeineMg)
            assertEquals(emptyList<CaffeineIntake>(), state.todayIntakes)
            assertFalse(state.isLimitExceeded)
            assertEquals(0f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding drink should call repository addIntake`() = runTest {
        every { repository.getTodayIntakes() } returns flowOf(emptyList())
        every { repository.getTodayTotalCaffeine() } returns flowOf(0)
        coEvery { repository.addIntake(any()) } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val drink = CaffeineDrink("espresso", 0, 63, 30)
        viewModel.addDrink(drink)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addIntake(any()) }
    }

    @Test
    fun `state should reflect total caffeine from repository`() = runTest {
        val intakes = listOf(
            CaffeineIntake(1, "espresso", 63, 30, 0L),
            CaffeineIntake(2, "black_coffee", 95, 250, 0L)
        )
        every { repository.getTodayIntakes() } returns flowOf(intakes)
        every { repository.getTodayTotalCaffeine() } returns flowOf(158)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(158, state.totalCaffeineMg)
            assertEquals(0.395f, state.progress, 0.001f)
            assertFalse(state.isLimitExceeded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `limit exceeded should be true when total over 400mg`() = runTest {
        every { repository.getTodayIntakes() } returns flowOf(emptyList())
        every { repository.getTodayTotalCaffeine() } returns flowOf(450)

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
        every { repository.getTodayIntakes() } returns flowOf(emptyList())
        every { repository.getTodayTotalCaffeine() } returns flowOf(800)

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll should call repository clearAll`() = runTest {
        every { repository.getTodayIntakes() } returns flowOf(emptyList())
        every { repository.getTodayTotalCaffeine() } returns flowOf(0)
        coEvery { repository.clearAll() } just Runs

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.clearAll() }
    }

    @Test
    fun `state should update when repository emits new data`() = runTest {
        val intakesFlow = MutableSharedFlow<List<CaffeineIntake>>(replay = 1)
        val totalFlow = MutableSharedFlow<Int>(replay = 1)
        intakesFlow.tryEmit(listOf(CaffeineIntake(1, "espresso", 63, 30, 0L)))
        totalFlow.tryEmit(63)

        every { repository.getTodayIntakes() } returns intakesFlow
        every { repository.getTodayTotalCaffeine() } returns totalFlow

        viewModel = CaffeineViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(63, awaitItem().totalCaffeineMg)

            intakesFlow.tryEmit(
                listOf(
                    CaffeineIntake(1, "espresso", 63, 30, 0L),
                    CaffeineIntake(2, "black_coffee", 95, 250, 0L)
                )
            )
            totalFlow.tryEmit(158)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(158, awaitItem().totalCaffeineMg)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
