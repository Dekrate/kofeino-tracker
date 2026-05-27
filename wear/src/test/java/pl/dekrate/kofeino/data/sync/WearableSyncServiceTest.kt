package pl.dekrate.kofeino.data.sync

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowService
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.common.sync.SyncStatus

/**
 * Unit tests for [WearableSyncService] using Robolectric + MockK + Turbine.
 *
 * The service uses `@AndroidEntryPoint` with `@Inject` fields that Hilt
 * normally provides.  These tests instantiate the service via
 * [Robolectric.buildService] and inject the dependencies via reflection so
 * that no Hilt test infrastructure is required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WearableSyncServiceTest {

    private lateinit var wearableDataLayerManager: WearableDataLayerManager

    private lateinit var syncStatusTracker: SyncStatusTracker

    private lateinit var notificationManager: NotificationManager
    private lateinit var service: WearableSyncService
    private lateinit var controller: ServiceController<WearableSyncService>
    private lateinit var shadowService: ShadowService
    private var destroyed = false

    @Before
    fun setUp() {
        // Create mocks explicitly.
        wearableDataLayerManager = mockk()
        every { wearableDataLayerManager.register() } just runs
        every { wearableDataLayerManager.unregister() } just runs

        syncStatusTracker = mockk(relaxed = true)
        every { syncStatusTracker.status } returns MutableStateFlow(SyncStatus.AwaitingDevice)

        controller = Robolectric.buildService(WearableSyncService::class.java)
        service = controller.get()
        destroyed = false

        // Use the real NotificationManager from the application context so that
        // ShadowNotificationManager can verify channel creation.
        notificationManager =
            service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Inject notificationManager BEFORE create() so onCreate() can use it.
        // Hilt's bytecode transformation (Hilt_WearableSyncService) will call
        // inject() during super.onCreate(), overwriting fields with Hilt-provided
        // instances.  That is acceptable for notificationManager (same system service).
        injectField("notificationManager", notificationManager)

        controller.create()
        shadowService = shadowOf(service)

        // AFTER create(), overwrite Hilt-provided instances with our mocks
        // so that tests can control their behaviour.
        injectField("wearableDataLayerManager", wearableDataLayerManager)
        injectField("syncStatusTracker", syncStatusTracker)
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized && !destroyed) {
            controller.destroy()
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Sets a private field via reflection. */
    private fun injectField(name: String, value: Any) {
        val field = WearableSyncService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    /** Sends [WearableSyncService.ACTION_START_SYNC] to the service. */
    private fun startSync() {
        val intent = Intent().apply { action = WearableSyncService.ACTION_START_SYNC }
        service.onStartCommand(intent, 0, 0)
    }

    /** Sends [WearableSyncService.ACTION_STOP_SYNC] to the service. */
    private fun stopSync() {
        val intent = Intent().apply { action = WearableSyncService.ACTION_STOP_SYNC }
        service.onStartCommand(intent, 0, 0)
    }

    // ------------------------------------------------------------------
    // 1. Service Lifecycle
    // ------------------------------------------------------------------

    @Test
    fun `onCreate sets initial sync status from SyncStatusTracker`() {
        // The syncStatus flow is a direct delegation to syncStatusTracker.status,
        // so both should expose the same value after onCreate.
        assertEquals(
            syncStatusTracker.status.value,
            service.syncStatus.value
        )
    }

    @Test
    fun `onDestroy unregisters wearableDataLayerManager`() {
        destroyed = true
        controller.destroy()
        verify(exactly = 1) { wearableDataLayerManager.unregister() }
    }

    // ------------------------------------------------------------------
    // 2. Intent Actions
    // ------------------------------------------------------------------

    @Test
    fun `ACTION_START_SYNC calls startForeground and registers WearableDataLayerManager`() {
        startSync()

        // startForeground was called
        assertNotNull(
            "Foreground notification must be set after ACTION_START_SYNC",
            shadowService.lastForegroundNotification
        )

        // WearableDataLayerManager was registered
        verify(exactly = 1) { wearableDataLayerManager.register() }
    }

    @Test
    fun `ACTION_START_SYNC returns START_STICKY even if register fails`() {
        every { wearableDataLayerManager.register() } throws RuntimeException("Register failed")

        val result = service.onStartCommand(
            Intent().apply { action = WearableSyncService.ACTION_START_SYNC },
            0,
            0
        )

        assertEquals(
            "onStartCommand must return START_STICKY regardless of errors",
            Service.START_STICKY,
            result
        )
    }

    @Test
    fun `ACTION_STOP_SYNC unregisters manager, stops foreground, and stops self`() {
        startSync()
        assertNotNull(
            "Notification must be present after start",
            shadowService.lastForegroundNotification
        )

        stopSync()

        // unregister was called on the manager
        verify(atLeast = 1) { wearableDataLayerManager.unregister() }

        // stopForeground(STOP_FOREGROUND_REMOVE) clears the notification
        assertNull(
            "Foreground notification must be removed after ACTION_STOP_SYNC",
            shadowService.lastForegroundNotification
        )
    }

    @Test
    fun `unknown action is logged and does nothing`() {
        val result = service.onStartCommand(
            Intent().apply { action = "pl.dekrate.kofeino.action.UNKNOWN" },
            0,
            0
        )

        assertEquals(
            "Unknown action must return START_STICKY",
            Service.START_STICKY,
            result
        )

        // No lifecycle calls were made on the manager
        verify(exactly = 0) { wearableDataLayerManager.register() }
        verify(exactly = 0) { wearableDataLayerManager.unregister() }
    }

    // ------------------------------------------------------------------
    // 3. Notification
    // ------------------------------------------------------------------

    @Test
    fun `createNotificationChannel creates channel on API 26+`() {
        // Query the system NotificationManager directly; Robolectric's shadow
        // intercepts the call so we can verify channel creation.
        val channel = notificationManager.getNotificationChannel(
            WearableSyncService.CHANNEL_ID
        )

        assertNotNull("Notification channel must be created on API 26+", channel)
        assertEquals(
            "Channel ID must match the constant",
            WearableSyncService.CHANNEL_ID,
            channel.id
        )
        assertEquals(
            "Channel importance must be LOW for persistent foreground indicator",
            NotificationManager.IMPORTANCE_LOW,
            channel.importance
        )
    }

    @Test
    fun `buildNotification returns notification with correct title and channel`() {
        startSync()

        val notification = shadowService.lastForegroundNotification
        assertNotNull("Notification must be set after start foreground", notification)

        assertEquals(
            "Notification channel ID must match the service constant",
            WearableSyncService.CHANNEL_ID,
            notification!!.channelId
        )

        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        assertEquals(
            "Notification title must come from string resource",
            service.getString(R.string.sync_notification_title),
            title
        )
    }

    // ------------------------------------------------------------------
    // 4. Binder
    // ------------------------------------------------------------------

    @Test
    fun `WearableSyncBinder returns the sync status flow`() {
        val binder = service.WearableSyncBinder()
        assertSame(
            syncStatusTracker.status,
            binder.getSyncStatusFlow()
        )
    }

    // ------------------------------------------------------------------
    // 5. onStartCommand return value
    // ------------------------------------------------------------------

    @Test
    fun `onStartCommand returns START_STICKY`() {
        val result = service.onStartCommand(
            Intent().apply { action = WearableSyncService.ACTION_START_SYNC },
            0,
            0
        )

        assertEquals(Service.START_STICKY, result)
    }

    // ------------------------------------------------------------------
    // 6. Multiple lifecycle transitions
    // ------------------------------------------------------------------

    @Test
    fun `multiple start-stop cycles are safe`() {
        repeat(3) {
            startSync()
            stopSync()
        }
        // No crash — pass
        assertTrue("Multiple start-stop cycles completed without exception", true)
    }

    // ------------------------------------------------------------------
    // 7. Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `ACTION_START_SYNC when already started is handled gracefully`() {
        startSync()
        // Second start — handleStartSync() is idempotent
        startSync()

        // register() should be called exactly twice (once per startSync call)
        verify(exactly = 2) { wearableDataLayerManager.register() }
    }

    @Test
    fun `onDestroy safety net unregisters if not stopped`() {
        startSync()

        // Destroy without calling stopSync first
        destroyed = true
        controller.destroy()

        // onDestroy safety net calls unregister()
        verify(exactly = 1) { wearableDataLayerManager.unregister() }
    }

    @Test
    fun `onStartCommand with null intent does not crash and returns START_STICKY`() {
        val result = service.onStartCommand(null, 0, 0)
        assertEquals(Service.START_STICKY, result)
    }

    @Test
    fun `onBind returns a WearableSyncBinder`() {
        val binder = service.onBind(Intent())
        assertTrue("Binder must be WearableSyncBinder", binder is WearableSyncService.WearableSyncBinder)
    }
}
