package pl.dekrate.kofeino.tracker.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import pl.dekrate.kofeino.tracker.MainActivity
import pl.dekrate.kofeino.tracker.R
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that hosts the cross-device sync lifecycle on the phone.
 *
 * ## Why a foreground service?
 *
 * Android aggressively background-kills apps that need to maintain long-lived
 * connections. Without a foreground service, the [WearableDataLayerManager]
 * listeners would be killed while the app is in the background, causing
 * missed sync events — including incoming data from the watch that the
 * phone never processes until the user manually opens the app.
 *
 * ## Design
 *
 * - **START_STICKY**: The system restarts the service if it is killed,
 *   ensuring persistent background sync. [WearableDataLayerManager.register]
 *   is idempotent, so a null restart intent is safe.
 * - **Low-importance notification**: Silent, no sound or vibration.
 *   The channel is created in [onCreate] to satisfy API 26+ requirements.
 * - **Delegation**: All sync processing delegates to existing injectable
 *   components. This service only manages lifecycle and tracks per-device
 *   sync state via [SyncDeviceStateManager].
 * - **Mid-sync disconnect**: Handled by [SyncDeviceStateManager.onDeviceDisconnected]
 *   — marks the device for full resync on reconnect. Room's
 *   [androidx.room.withTransaction] in [IncomingSyncProcessor] ensures
 *   atomic batch processing, so no DB-level rollback is needed here.
 *
 * ## Lifecycle
 *
 * ```
 * onCreate() ─► createNotificationChannel()
 *                  │
 * onStartCommand() ┤── startForeground(NOTIFICATION_ID, notification)
 *                  │── wearableDataLayerManager.register()
 *                  └── return START_STICKY
 *                     │
 * onDestroy() ─────────┤── wearableDataLayerManager.unregister()
 *                      └── serviceScope.cancel()
 * ```
 *
 * ## Usage
 *
 * Start from [KofeinoTrackerApplication.onCreate]:
 * ```kotlin
 * val intent = WearableSyncService.startIntent(this)
 * ContextCompat.startForegroundService(this, intent)
 * ```
 */
@AndroidEntryPoint
class WearableSyncService : Service() {

    @Inject
    lateinit var wearableDataLayerManager: WearableDataLayerManager

    @Inject
    lateinit var syncDeviceStateManager: SyncDeviceStateManager

    @Inject
    lateinit var syncStatusTracker: SyncStatusTracker

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val binder = LocalBinder()

    // ── Service Lifecycle ─────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("WearableSyncService: onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("WearableSyncService: onStartCommand intent=%s flags=%d startId=%d",
            intent, flags, startId)

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Register DataLayer listeners (idempotent — safe to call multiple times)
        wearableDataLayerManager.register()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("WearableSyncService: onBind %s", intent)
        return binder
    }

    override fun onDestroy() {
        Timber.d("WearableSyncService: onDestroy")
        wearableDataLayerManager.unregister()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Foreground Notification ───────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sync_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.sync_notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(getString(R.string.sync_notification_text))
            .setSmallIcon(R.drawable.ic_notification_caffeine)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ── Binder ────────────────────────────────────────────────

    /**
     * Local binder for activity-bound communication.
     * Allows bound activities to query sync state directly from the service.
     */
    inner class LocalBinder : android.os.Binder() {
        fun getService(): WearableSyncService = this@WearableSyncService
    }

    companion object {
        /** Notification channel ID for the sync foreground notification. */
        const val CHANNEL_ID = "wearable_sync_service"

        /** Notification ID for the sync foreground notification. */
        const val NOTIFICATION_ID = 1001

        /** Create an Intent to start this service. */
        @JvmStatic
        fun startIntent(context: Context): Intent {
            return Intent(context, WearableSyncService::class.java)
        }
    }
}
