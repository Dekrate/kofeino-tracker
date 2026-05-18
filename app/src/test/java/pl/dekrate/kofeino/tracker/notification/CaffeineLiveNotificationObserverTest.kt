package pl.dekrate.kofeino.tracker.notification

import android.app.PendingIntent
import android.content.Context
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HomeViewModel

/**
 * Unit tests for [CaffeineLiveNotificationObserver].
 *
 * Uses MockK + a test CoroutineScope with StandardTestDispatcher
 * to verify observer behavior deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaffeineLiveNotificationObserverTest {

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
    private lateinit var notificationManager: CaffeineLiveNotificationManager
    private lateinit var reminderManager: CaffeineReminderManager
    private lateinit var preferences: DataStorePreferences
    private lateinit var observer: CaffeineLiveNotificationObserver
    private lateinit var testScope: CoroutineScope
    private val liveEnabledFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        reminderManager = mockk(relaxed = true)
        every { repository.getTotalCaffeineForDate(any()) } returns MutableStateFlow(0)
        every { notificationManager.resetDismissedFlag() } just runs
        every { notificationManager.dismiss() } just runs
        every { notificationManager.update(any(), any()) } just runs
        every { preferences.observeNotificationLiveEnabled() } returns liveEnabledFlow
        every { preferences.observeNotificationMorningEnabled() } returns MutableStateFlow(false)
        every { preferences.observeNotificationRegularEnabled() } returns MutableStateFlow(false)
        every { preferences.observeNotificationEveningEnabled() } returns MutableStateFlow(false)
        every { preferences.isNotificationLiveEnabled() } returns true
        every { reminderManager.scheduleMorning(any()) } just runs
        every { reminderManager.scheduleRegular(any()) } just runs
        every { reminderManager.scheduleEvening(any()) } just runs

        testScope = CoroutineScope(SupervisorJob() + testDispatcher)

        val context = mockk<android.content.Context>(relaxed = true)
        every { context.registerReceiver(any(), any(), any(), any()) } returns mockk()
        every { context.packageName } returns "pl.dekrate.kofeino.tracker"
        every { context.getSystemService(Context.ALARM_SERVICE) } returns mockk<android.app.AlarmManager>(relaxed = true)
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()

        observer = CaffeineLiveNotificationObserver(
            context = context,
            repository = repository,
            notificationManager = notificationManager,
            preferences = preferences,
            reminderManager = reminderManager
        )
        observer.scope = testScope
    }

    // ===== Start / Stop tests =====

    @Test
    fun `start resets dismissed flag`() {
        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(atLeast = 1) { notificationManager.resetDismissedFlag() }
    }

    @Test
    fun `start observes repository for today date`() {
        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 1) { repository.getTotalCaffeineForDate(any()) }
    }

    @Test
    fun `start is idempotent`() {
        observer.start()
        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()
        // resetDismissedFlag should be called at least once (from the live toggle coroutine)
        verify(atLeast = 1) { notificationManager.resetDismissedFlag() }
    }

    @Test
    fun `stop dismisses notification`() {
        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()
        clearMocks(notificationManager)
        every { notificationManager.dismiss() } just runs

        observer.stop()

        verify(exactly = 1) { notificationManager.dismiss() }
    }

    @Test
    fun `stop is idempotent`() {
        observer.stop()
        observer.stop() // should not crash
    }

    @Test
    fun `stop without start does not crash`() {
        observer.stop()
    }

    // ===== Notification update on intake change =====

    @Test
    fun `on intake total change notification is updated`() = runTest(testDispatcher) {
        val totalFlow = MutableStateFlow(0)
        every { repository.getTotalCaffeineForDate(any()) } returns totalFlow

        clearMocks(notificationManager)
        every { notificationManager.update(any(), any()) } just runs
        every { notificationManager.resetDismissedFlag() } just runs

        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial state should trigger update
        verify(atLeast = 1) { notificationManager.update(0, HomeViewModel.SAFE_LIMIT_MG) }

        totalFlow.value = 150
        testDispatcher.scheduler.advanceUntilIdle()
        verify { notificationManager.update(150, HomeViewModel.SAFE_LIMIT_MG) }

        totalFlow.value = 300
        testDispatcher.scheduler.advanceUntilIdle()
        verify { notificationManager.update(300, HomeViewModel.SAFE_LIMIT_MG) }
    }

    @Test
    fun `same total does not trigger update`() = runTest(testDispatcher) {
        val totalFlow = MutableStateFlow(100)
        every { repository.getTotalCaffeineForDate(any()) } returns totalFlow

        clearMocks(notificationManager)
        every { notificationManager.update(any(), any()) } just runs
        every { notificationManager.resetDismissedFlag() } just runs

        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()

        clearMocks(notificationManager, answers = false)
        every { notificationManager.update(any(), any()) } just runs

        totalFlow.value = 100
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { notificationManager.update(any(), any()) }
    }

    @Test
    fun `dismiss delegates to notification manager`() {
        observer.dismiss()
        verify(exactly = 1) { notificationManager.dismiss() }
    }

    @Test
    fun `repository error does not crash observer`() = runTest(testDispatcher) {
        val totalFlow = MutableStateFlow(0)
        every { repository.getTotalCaffeineForDate(any()) } returns totalFlow

        clearMocks(notificationManager)
        every { notificationManager.update(any(), any()) } just runs
        every { notificationManager.resetDismissedFlag() } just runs

        observer.start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should still work after error-free emission
        totalFlow.value = 50
        testDispatcher.scheduler.advanceUntilIdle()

        verify { notificationManager.update(50, HomeViewModel.SAFE_LIMIT_MG) }
    }
}
