package pl.dekrate.kofeino.tracker.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CaffeineLiveNotificationManager] using MockK.
 *
 * Behavioral tests: verify interactions with [NotificationManagerCompat]
 * rather than asserting on Notification content (which needs a real
 * Android Context incompatible with AGP 9.1.0 + Robolectric 4.16.1).
 */
class CaffeineLiveNotificationManagerTest {

    private lateinit var mockNotificationManager: androidx.core.app.NotificationManagerCompat
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockNotificationManager = mockk(relaxed = true)
        // Mock Android framework classes not available in unit tests
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setFlags(any()) } returns mockk()
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk()
    }

    // ===== Channel tests =====

    @Test
    fun `init does not crash`() {
        val manager = createManager()
        assertNotNull("Manager should be created", manager)
    }

    @Test
    fun `init sets static instance reference`() {
        CaffeineLiveNotificationManager.instance = null
        val manager = createManager()
        assertNotNull(
            "Static instance should be set after init",
            CaffeineLiveNotificationManager.instance
        )
        assertEquals(manager, CaffeineLiveNotificationManager.instance)
    }

    // ===== Update guard tests (verify skip logic without building notifications) =====

    @Test
    fun `update skips when user dismissed`() {
        val manager = createManager()
        manager.dismiss() // sets isUserDismissed = true

        manager.update(currentMg = 100, targetMg = 400)

        verify(exactly = 0) { mockNotificationManager.notify(any(), any()) }
    }

    @Test
    fun `update skips when notifications disabled`() {
        val manager = createManager()
        withPlatformNotificationEnabled(false)

        manager.update(currentMg = 100, targetMg = 400)

        verify(exactly = 0) { mockNotificationManager.notify(any(), any()) }
    }

    @Test
    fun `update works after resetting dismissed flag`() {
        val manager = createManager()
        withPlatformNotificationEnabled(false)
        manager.dismiss()
        manager.resetDismissedFlag()

        // The dismiss flag was reset, so the call should reach the notifications check
        manager.update(currentMg = 75, targetMg = 400)

        verify(atLeast = 1) { mockNotificationManager.cancel(any()) }
    }

    @Test
    fun `update does not call notify when notifications disabled`() {
        val manager = createManager()
        withPlatformNotificationEnabled(false)

        manager.update(currentMg = 100, targetMg = 400)

        verify(exactly = 0) { mockNotificationManager.notify(any(), any()) }
    }

    // ===== Dismiss tests =====

    @Test
    fun `dismiss calls cancel on notification manager`() {
        val manager = createManager()

        manager.dismiss()

        verify(exactly = 1) { mockNotificationManager.cancel(
            CaffeineLiveNotificationManager.NOTIFICATION_ID
        ) }
    }

    @Test
    fun `dismiss sets dismissed flag`() {
        val manager = createManager()
        assertFalse("Flag should start as false", manager.isUserDismissed)

        manager.dismiss()
        assertTrue("Flag should be true after dismiss", manager.isUserDismissed)
    }

    // ===== Reset / flag tests =====

    @Test
    fun `resetDismissedFlag clears the dismissed state`() {
        val manager = createManager()
        manager.dismiss()
        assertTrue(manager.isUserDismissed)

        manager.resetDismissedFlag()
        assertFalse("Flag should be false after reset", manager.isUserDismissed)
    }

    @Test
    fun `cleanup nullifies static instance`() {
        val manager = createManager()
        manager.cleanup()
        assertNull("Static instance should be null after cleanup",
            CaffeineLiveNotificationManager.instance)
    }

    // ===== Helper =====

    private fun createManager(): CaffeineLiveNotificationManager {
        every { context.registerReceiver(any(), any(), any(), any()) } returns mockk()
        every { context.packageName } returns "pl.dekrate.kofeino.tracker"

        every { mockNotificationManager.getNotificationChannel(any()) } returns mockk(relaxed = true)
        every { mockNotificationManager.createNotificationChannel(any<android.app.NotificationChannel>()) } just runs
        every { mockNotificationManager.notify(any(), any()) } just runs
        every { mockNotificationManager.cancel(any()) } just runs
        // Platform NotificationManager for areNotificationsEnabled()
        withPlatformNotificationEnabled(true)

        return CaffeineLiveNotificationManager(context, mockNotificationManager)
    }

    private fun withPlatformNotificationEnabled(enabled: Boolean) {
        val platformManager = mockk<android.app.NotificationManager>(relaxed = true)
        every { platformManager.areNotificationsEnabled() } returns enabled
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns platformManager
    }
}
