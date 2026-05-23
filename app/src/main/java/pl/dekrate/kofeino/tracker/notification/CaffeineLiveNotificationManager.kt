package pl.dekrate.kofeino.tracker.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.dekrate.kofeino.tracker.MainActivity
import pl.dekrate.kofeino.tracker.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the live caffeine notification — the **Now Brief** for your daily intake.
 *
 * **SOLID:**
 * - Single Responsibility: owns notification channel, building, update, and teardown.
 * - Open/Closed: extend via new [Notification.Style] types without modifying this class.
 *
 * On Android 16+ the notification uses [Notification.ProgressStyle] to render as a
 * Live Update card in the lock screen / notification shade — Samsung Now Brief style.
 * Older API levels degrade gracefully to a standard ongoing progress notification.
 */
@Singleton
class CaffeineLiveNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat
) {

    companion object {
        const val CHANNEL_ID = "kofeino_live"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "pl.dekrate.kofeino.tracker.action.DISMISS_LIVE"
        private const val TAG = "CaffeineNotifMgr"

        /** Static reference for system-triggered receivers (MidnightReceiver). */
        @Volatile
        internal var instance: CaffeineLiveNotificationManager? = null
    }

    // ── Dismiss receiver ──────────────────────────────────────────────────

    private val dismissReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_DISMISS) {
                Timber.tag(TAG).i("User dismissed live notification")
                isUserDismissed = true
            }
        }
    }

    @Volatile
    var isUserDismissed: Boolean = false
        private set

    init {
        createChannel()
        instance = this
        context.registerReceiver(dismissReceiver, IntentFilter(ACTION_DISMISS),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    // ── Channel ───────────────────────────────────────────────────────────

    private fun createChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
        Timber.tag(TAG).i("Channel '$CHANNEL_ID' created (IMPORTANCE_LOW)")
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Shows or updates the live caffeine brief.
     *
     * @param currentMg total caffeine consumed today
     * @param targetMg  daily target (default 400 mg)
     */
    @SuppressLint("MissingPermission")
    fun update(currentMg: Int, targetMg: Int) {
        if (isUserDismissed) {
            Timber.tag(TAG).d("Skipping update — user dismissed")
            return
        }
        // Use platform API directly (NotificationManagerCompat has issues on API 36)
        val platformManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!platformManager.areNotificationsEnabled()) {
            Timber.tag(TAG).w("Notifications disabled — skipping update")
            return
        }
        val notification = buildNotification(currentMg, targetMg)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /** Dismisses the notification and marks it as user-dismissed. */
    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
        isUserDismissed = true
        Timber.tag(TAG).i("Notification dismissed")
    }

    /** Clears the dismissed flag so the notification can reappear on a new day. */
    fun resetDismissedFlag() {
        isUserDismissed = false
    }

    // ── Notification building ─────────────────────────────────────────────

    private fun buildNotification(currentMg: Int, targetMg: Int): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(ACTION_DISMISS).apply { setPackage(context.packageName) }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isOverLimit = currentMg > targetMg

        // ── Android 16+ — platform API with ProgressStyle for Live Updates ─
        if (Build.VERSION.SDK_INT >= 36) {
            return buildApi36Notification(
                currentMg, targetMg, isOverLimit,
                openPendingIntent, dismissPendingIntent
            )
        }

        // ── Older API — NotificationCompat with progress bar ─────────────
        return buildCompatNotification(
            currentMg, targetMg, isOverLimit,
            openPendingIntent, dismissPendingIntent
        )
    }

    @SuppressLint("NewApi")
    private fun buildApi36Notification(
        currentMg: Int, targetMg: Int, isOverLimit: Boolean,
        openIntent: PendingIntent, dismissIntent: PendingIntent
    ): Notification {
        // ProgressStyle.setProgress(int) uses a 0-100 scale internally
        val pct = if (targetMg > 0) {
            (currentMg.toFloat() / targetMg.toFloat() * 100).toInt().coerceIn(0, 100)
        } else 0

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_caffeine)
            .setContentTitle(
                if (isOverLimit) context.getString(R.string.notification_title_limit_exceeded)
                else context.getString(R.string.notification_title)
            )
            .setContentText(
                if (isOverLimit) context.getString(R.string.notification_body_over, currentMg)
                else context.getString(R.string.notification_body, currentMg, targetMg)
            )
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setDeleteIntent(dismissIntent)
            .setOnlyAlertOnce(true)
            .setStyle(
                Notification.ProgressStyle()
                    .setProgress(pct)
            )
            // Status bar critical text (API 33+)
            .setShortCriticalText(
                context.getString(R.string.notification_short_text, currentMg)
            )
            .build()
    }

    private fun buildCompatNotification(
        currentMg: Int, targetMg: Int, isOverLimit: Boolean,
        openIntent: PendingIntent, dismissIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_caffeine)
            .setContentTitle(
                if (isOverLimit) context.getString(R.string.notification_title_limit_exceeded)
                else context.getString(R.string.notification_title)
            )
            .setContentText(
                if (isOverLimit) context.getString(R.string.notification_body_over, currentMg)
                else context.getString(R.string.notification_body, currentMg, targetMg)
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .setDeleteIntent(dismissIntent)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setProgress(targetMg, currentMg, false)
            .build()
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /** Unregisters the dismiss receiver. Call when the app no longer needs live updates. */
    fun cleanup() {
        try {
            context.unregisterReceiver(dismissReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered
        }
        instance = null
    }
}
