package pl.dekrate.kofeino.notification

import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.dekrate.kofeino.data.local.NotificationPreferences

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WearNotificationObserverTest {

    private lateinit var preferences: NotificationPreferences
    private lateinit var reminderManager: CaffeineReminderManager
    private lateinit var observer: WearNotificationObserver

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        preferences = mockk(relaxed = true) {
            every { morningFlow } returns flowOf(false)
            every { regularFlow } returns flowOf(false)
            every { eveningFlow } returns flowOf(false)
        }
        reminderManager = mockk(relaxed = true)
        observer = WearNotificationObserver(context, preferences, reminderManager)
    }

    @After
    fun tearDown() {
        if (observer.scope.isActive) {
            observer.stop()
        }
    }

    @Test
    fun `start creates an active coroutine scope`() {
        observer.start()
        assertTrue(observer.scope.isActive)
    }

    @Test
    fun `stop cancels the coroutine scope`() {
        observer.start()
        val oldScope = observer.scope
        observer.stop()
        assertFalse(oldScope.isActive)
    }

    @Test
    fun `stop creates a new active scope for restart`() {
        observer.start()
        observer.stop()
        assertTrue(observer.scope.isActive)
    }

    @Test
    fun `start is idempotent when called multiple times`() {
        observer.start()
        val scopeAfterFirstStart = observer.scope
        observer.start()
        val scopeAfterSecondStart = observer.scope
        assertTrue(scopeAfterFirstStart === scopeAfterSecondStart)
    }

    @Test
    fun `stop then start reuses a fresh scope`() {
        observer.start()
        val oldScope = observer.scope
        observer.stop()
        observer.start()
        val newScope = observer.scope
        assertFalse(oldScope.isActive)
        assertTrue(newScope.isActive)
        assertTrue(oldScope !== newScope)
    }

    @Test
    fun `multiple start-stop cycles do not leak coroutines`() {
        repeat(5) {
            observer.start()
            observer.stop()
        }
        assertTrue(observer.scope.isActive)
    }

    @Test
    fun `scope is cancelled before reassignment in start`() {
        observer.start()
        val oldScope = observer.scope
        observer.stop()
        observer.start()
        assertFalse("Old scope must be cancelled", oldScope.isActive)
        assertTrue("New scope must be active", observer.scope.isActive)
    }

    @Test
    fun `stop is safe when not started`() {
        observer.stop()
        assertTrue(observer.scope.isActive)
    }

    @Test
    fun `start triggers reminder scheduling`() {
        observer.start()
        verify { reminderManager.scheduleMorning(false) }
    }
}
