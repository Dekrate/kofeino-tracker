package pl.dekrate.kofeino.tracker.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class EditIntakeViewModelTest {

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
    private lateinit var viewModel: EditIntakeViewModel

    private val sampleIntake = CaffeineIntake(
        id = 1,
        drinkId = 1,
        drinkName = "Espresso",
        caffeineMg = 63,
        volumeMl = 30,
        timestamp = 1000L
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    // ===== loadIntake tests =====

    @Test
    fun `loadIntake should populate state from repository`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false after load", state.isLoading)
        assertEquals("Espresso", state.drinkName)
        assertEquals(63, state.caffeineMg)
        assertEquals(30, state.volumeMl)
        assertNotNull("intake should be set", state.intake)
    }

    @Test
    fun `loadIntake should show loading state initially`() = runTest {
        // Use a long-running suspend to keep loading true
        coEvery { repository.getIntakeById(1L) } coAnswers {
            kotlinx.coroutines.delay(1000)
            sampleIntake
        }

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)

        assertTrue("isLoading should be true before completion", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadIntake should set error when intake not found`() = runTest {
        coEvery { repository.getIntakeById(99L) } returns null

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(99L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false", state.isLoading)
        assertNotNull("error should be set", state.error)
        assertNull("intake should be null", state.intake)
    }

    @Test
    fun `loadIntake should handle repository exception`() = runTest {
        coEvery { repository.getIntakeById(1L) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false after error", state.isLoading)
        assertNotNull("error should be set on exception", state.error)
    }

    // ===== updateCaffeineMg tests =====

    @Test
    fun `updateCaffeineMg with +5 should increase caffeine`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCaffeineMg(5)

        assertEquals(68, viewModel.uiState.value.caffeineMg)
    }

    @Test
    fun `updateCaffeineMg with -5 should decrease caffeine`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCaffeineMg(-5)

        assertEquals(58, viewModel.uiState.value.caffeineMg)
    }

    @Test
    fun `updateCaffeineMg should not go below 0`() = runTest {
        val lowCaffeine = sampleIntake.copy(caffeineMg = 3)
        coEvery { repository.getIntakeById(1L) } returns lowCaffeine

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCaffeineMg(-5)

        assertEquals(0, viewModel.uiState.value.caffeineMg)
    }

    // ===== updateVolumeMl tests =====

    @Test
    fun `updateVolumeMl with +10 should increase volume`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateVolumeMl(10)

        assertEquals(40, viewModel.uiState.value.volumeMl)
    }

    @Test
    fun `updateVolumeMl with -10 should decrease volume`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateVolumeMl(-10)

        assertEquals(20, viewModel.uiState.value.volumeMl)
    }

    @Test
    fun `updateVolumeMl should not go below 0`() = runTest {
        val lowVolume = sampleIntake.copy(volumeMl = 5)
        coEvery { repository.getIntakeById(1L) } returns lowVolume

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateVolumeMl(-10)

        assertEquals(0, viewModel.uiState.value.volumeMl)
    }

    // ===== save tests =====

    @Test
    fun `save should call repository updateIntake with modified values`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.updateIntake(any()) } returns Unit

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Modify values
        viewModel.updateCaffeineMg(5)  // 68
        viewModel.updateVolumeMl(10)   // 40

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.updateIntake(match {
                it.id == 1L &&
                it.caffeineMg == 68 &&
                it.volumeMl == 40 &&
                it.drinkName == "Espresso"
            })
        }
    }

    @Test
    fun `save should call onComplete on success`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.updateIntake(any()) } returns Unit

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.save(onComplete = { completed = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete should be called", completed)
    }

    @Test
    fun `save should set error on repository exception`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.updateIntake(any()) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error should be set on save failure", viewModel.uiState.value.error)
    }

    @Test
    fun `save should call onError on repository exception`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.updateIntake(any()) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        var errorCalled = false
        viewModel.save(onError = { errorCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onError should be called", errorCalled)
    }

    // ===== delete tests =====

    @Test
    fun `delete should call repository deleteIntake`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.deleteIntake(any()) } returns Unit

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.delete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteIntake(sampleIntake) }
    }

    @Test
    fun `delete should call onComplete on success`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.deleteIntake(any()) } returns Unit

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.delete(onComplete = { completed = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete should be called", completed)
    }

    @Test
    fun `delete should set error on repository exception`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.deleteIntake(any()) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.delete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error should be set on delete failure", viewModel.uiState.value.error)
    }

    @Test
    fun `delete should call onError on repository exception`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake
        coEvery { repository.deleteIntake(any()) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        var errorCalled = false
        viewModel.delete(onError = { errorCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onError should be called", errorCalled)
    }

    // ===== hasChanges tests =====

    @Test
    fun `hasChanges should return false when values match original`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("hasChanges should be false before any modification", viewModel.hasChanges())
    }

    @Test
    fun `hasChanges should return true when caffeine is modified`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCaffeineMg(5)

        assertTrue("hasChanges should be true after caffeine change", viewModel.hasChanges())
    }

    @Test
    fun `hasChanges should return true when volume is modified`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateVolumeMl(10)

        assertTrue("hasChanges should be true after volume change", viewModel.hasChanges())
    }

    @Test
    fun `hasChanges should return false when intake is not loaded yet`() = runTest {
        viewModel = EditIntakeViewModel(repository)

        // hasChanges should not crash when intake is null
        assertFalse("hasChanges should be false when no intake loaded", viewModel.hasChanges())
    }

    // ===== clearError tests =====

    @Test
    fun `clearError should set error to null`() = runTest {
        coEvery { repository.getIntakeById(1L) } throws RuntimeException("DB error")

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should be null after clearError", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError should be idempotent`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("Error should remain null after multiple clearError calls", viewModel.uiState.value.error)
    }

    // ===== Interaction tests =====

    @Test
    fun `repository getIntakeById should be called with correct id`() = runTest {
        coEvery { repository.getIntakeById(42L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(42L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.getIntakeById(42L) }
    }

    @Test
    fun `loadIntake should be idempotent when called multiple times`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Espresso", state.drinkName)
        assertEquals(63, state.caffeineMg)
    }

    @Test
    fun `multiple adjustments should accumulate correctly`() = runTest {
        coEvery { repository.getIntakeById(1L) } returns sampleIntake

        viewModel = EditIntakeViewModel(repository)
        viewModel.loadIntake(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        repeat(3) { viewModel.updateCaffeineMg(5) }    // +15
        repeat(2) { viewModel.updateCaffeineMg(-5) }   // -10 → net +5
        repeat(4) { viewModel.updateVolumeMl(10) }      // +40

        assertEquals(68, viewModel.uiState.value.caffeineMg)  // 63 + 5
        assertEquals(70, viewModel.uiState.value.volumeMl)    // 30 + 40
    }
}
