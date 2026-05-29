package pl.dekrate.kofeino.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import pl.dekrate.kofeino.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.presentation.MainActivity
import timber.log.Timber
import javax.inject.Inject

/**
 * Android foreground service that manages the lifecycle of Wearable Data
 * Layer listeners for cross-device synchronisation.
 *
 * ## Why a foreground service?
 * On Wear OS, system-initiated background work is unreliable and may be
 * killed when the app moves to the background. A foreground service with a
 * persistent (low-importance) notification keeps the sync infrastructure
 * alive as long as the user wants cross-device sync.
 *
 * ## Lifecycle
 * - [ACTION_START_SYNC] — registers Wearable Data Layer listeners and shows
 *   the persistent foreground notification.
 * - [ACTION_STOP_SYNC] — unregisters all listeners, removes the notification,
 *   and stops the service.
 * - [onDestroy] — safety net: unregisters listeners if the service was
 *   killed without [ACTION_STOP_SYNC].
 *
 * ## Thread safety
 * All Wearable Data Layer operations are handled by
 * [WearableDataLayerManager] which uses its own [kotlinx.coroutines.CoroutineScope].
 * This service only delegates to it.
 */
@AndroidEntryPoint
class WearableSyncService : Service() {

    @Inject
    lateinit var wearableDataLayerManager: WearableDataLayerManager

    @Inject
    lateinit var syncStatusTracker: SyncStatusTracker

    @Inject
    lateinit var notificationManager: NotificationManager

    /**
     * Live sync status, delegated directly from [SyncStatusTracker.status].
     * Bound clients can collect this via [WearableSyncBinder].
     */
    val syncStatus: StateFlow<SyncStatus> get() = syncStatusTracker.status

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Timber.i("WearableSyncService created")
        createNotificationChannel()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Timber.d("WearableSyncService onStartCommand: action=%s flags=%d startId=%d",
            action, flags, startId)

        try {
            when {
                intent == null -> {
                    Timber.w("WearableSyncService: restarted by system with null intent — re-registering")
                    handleStartSync()
                }
                action == ACTION_START_SYNC -> handleStartSync()
                action == ACTION_STOP_SYNC -> handleStopSync()
                else -> {
                    Timber.w("WearableSyncService: unknown action=%s (defaulting to START_STICKY)", action)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "WearableSyncService: error handling action=%s", action)
            if (BuildConfig.DEBUG) throw e
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("WearableSyncService onBind")
        return WearableSyncBinder()
    }

    override fun onDestroy() {
        Timber.i("WearableSyncService destroyed — unregistering listeners")
        // WearableDataLayerManager.unregister() handles its own exceptions internally
        wearableDataLayerManager.unregister()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Action handlers
    // ------------------------------------------------------------------

    /**
     * Promotes the service to foreground and registers Wearable Data Layer
     * listeners. Idempotent — safe to call if already registered.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStartSync() {
        // Register listeners FIRST so the service is functional before
        // showing the persistent notification.
        try {
            wearableDataLayerManager.register()
            Timber.d("WearableSyncService: WearableDataLayerManager registered")
        } catch (e: Exception) {
            Timber.e(e, "WearableSyncService: failed to register — will not start foreground")
            return
        }

        // Only start foreground if registration succeeded.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            Timber.d("WearableSyncService: started foreground with notification id=%d", NOTIFICATION_ID)
        } catch (e: Exception) {
            Timber.e(e, "WearableSyncService: failed to start foreground")
        }
    }

    /**
     * Unregisters Wearable Data Layer listeners, removes the foreground
     * notification, and stops this service.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleStopSync() {
        try {
            wearableDataLayerManager.unregister()
            Timber.d("WearableSyncService: WearableDataLayerManager unregistered")
        } catch (e: Exception) {
            Timber.w(e, "WearableSyncService: error during unregister")
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            Timber.d("WearableSyncService: foreground stopped")
        } catch (e: Exception) {
            Timber.w(e, "WearableSyncService: error stopping foreground")
        }

        stopSelf()
        Timber.i("WearableSyncService: stopped self")
    }

    // ------------------------------------------------------------------
    // Notification
    // ------------------------------------------------------------------

    /**
     * Creates the low-importance notification channel required for foreground
     * service notifications on API 26+. Idempotent.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            Timber.d("WearableSyncService: notification channel '%s' already exists", CHANNEL_ID)
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sync_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.sync_channel_description)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
        Timber.d("WearableSyncService: created notification channel '%s'", CHANNEL_ID)
    }

    /**
     * Builds the persistent foreground notification for the sync service.
     *
     * Uses a low-importance channel so the notification is present but
     * not intrusive. Tapping the notification opens [MainActivity].
     */
    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_caffeine)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(getString(R.string.sync_notification_text))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ------------------------------------------------------------------
    // Binder
    // ------------------------------------------------------------------

    /**
     * Local Binder for bound clients that need to observe sync status.
     *
     * Usage in an Activity or Composable:
     * ```kotlin
     * val binder = (service as WearableSyncService).WearableSyncBinder()
     * binder.getSyncStatusFlow().collect { status -> ... }
     * ```
     */
    inner class WearableSyncBinder : Binder() {
        fun getSyncStatusFlow(): StateFlow<SyncStatus> = this@WearableSyncService.syncStatus
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    companion object {
        /** Start the sync foreground service. */
        const val ACTION_START_SYNC = "pl.dekrate.kofeino.action.START_SYNC"

        /** Stop the sync foreground service. */
        const val ACTION_STOP_SYNC = "pl.dekrate.kofeino.action.STOP_SYNC"

        /** Notification ID used for the foreground service notification. */
        const val NOTIFICATION_ID = 1001

        /** Notification channel used by the sync foreground service. */
        const val CHANNEL_ID = "sync_service"
    }
}
