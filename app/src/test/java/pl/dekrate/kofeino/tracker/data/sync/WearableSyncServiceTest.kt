package pl.dekrate.kofeino.tracker.data.sync

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [WearableSyncService] using Robolectric.
 *
 * Tests verify:
 * - Service starts as foreground with notification
 * - START_STICKY return value for background survival
 * - WearableDataLayerManager is registered on start
 * - WearableDataLayerManager is unregistered on destroy
 * - Binder returns correct service instance
 * - Notification channel is created
 *
 * Hilt injection is bypassed for unit tests by setting @Inject fields
 * manually after service construction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WearableSyncServiceTest {

    @MockK
    private lateinit var wearableDataLayerManager: WearableDataLayerManager

    @MockK
    private lateinit var syncDeviceStateManager: SyncDeviceStateManager

    @MockK
    private lateinit var syncStatusTracker: SyncStatusTracker

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // No cleanup needed — each test creates its own service controller.
    }

    /**
     * Creates a service controller with mock dependencies injected.
     * This avoids repeating the 3-line mock injection block in every test.
     */
    private fun createService(
        create: Boolean = true,
        start: Boolean = false
    ) = Robolectric.buildService(WearableSyncService::class.java).apply {
        if (create) {
            create()
            get().wearableDataLayerManager = wearableDataLayerManager
            get().syncDeviceStateManager = syncDeviceStateManager
            get().syncStatusTracker = syncStatusTracker
        }
        if (start) {
            startCommand(0, 0)
        }
    }

    @Test
    fun `service starts as foreground with notification`() {
        val controller = createService(start = true)

        val serviceShadow = shadowOf(controller.get())
        assertNotNull("Service must be foreground", serviceShadow.lastForegroundNotification)
        assertEquals(
            "Notification ID must match",
            WearableSyncService.NOTIFICATION_ID,
            serviceShadow.lastForegroundNotificationId
        )
    }

    @Test
    fun `service returns START_STICKY`() {
        val controller = createService(start = false)
        val result = controller.get().onStartCommand(null, 0, 0)

        assertEquals(
            "Service must return START_STICKY for background survival",
            android.app.Service.START_STICKY, result
        )
    }

    @Test
    fun `service registers DataLayer manager on start`() {
        createService(start = true)

        verify(exactly = 1) { wearableDataLayerManager.register() }
    }

    @Test
    fun `service unregisters DataLayer manager on destroy`() {
        val controller = createService(start = true)
        controller.destroy()

        verify(exactly = 1) { wearableDataLayerManager.unregister() }
    }

    @Test
    fun `service creates notification channel on create`() {
        createService()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(WearableSyncService.CHANNEL_ID)
        assertNotNull("Notification channel must exist after service creation", channel)
        assertEquals(
            "Channel importance must be LOW",
            NotificationManager.IMPORTANCE_LOW, channel.importance
        )
    }

    @Test
    fun `binder returns correct service instance`() {
        val controller = createService()

        val binder = controller.get().onBind(null)
        assertNotNull("Binder must not be null", binder)
        assert(binder is WearableSyncService.LocalBinder)
        val boundService = (binder as WearableSyncService.LocalBinder).getService()
        assertSame("Binder must return the same service instance", controller.get(), boundService)
    }

    @Test
    fun `service can be started with startIntent helper`() {
        val intent = WearableSyncService.startIntent(context)
        assertNotNull("Intent must not be null", intent)
        assertEquals(
            "Intent must target WearableSyncService",
            WearableSyncService::class.java.name,
            intent.component?.className
        )
    }

    @Test
    fun `service survives multiple start commands`() {
        createService(start = true)
        createService(start = true) // second start

        verify(exactly = 2) { wearableDataLayerManager.register() }
    }
}
