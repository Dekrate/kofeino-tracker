package pl.dekrate.kofeino.tracker.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.annotation.SuppressLint
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.dekrate.kofeino.tracker.MainActivity
import pl.dekrate.kofeino.tracker.R
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and shows reminder notifications (morning, regular, evening)
 * via [AlarmManager]. Each reminder type uses its own channel so users
 * can customise them individually in system settings.
 */
@Singleton
class CaffeineReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat
) {

    companion object {
        const val CHANNEL_ID_REMINDERS = "kofeino_reminders"
        private const val RC_MORNING = 10
        private const val RC_REGULAR = 11
        private const val RC_EVENING = 12
        private const val TAG = "CaffeineRemindMgr"
        private const val NIGHT_START_HOUR = 22
        private const val NIGHT_END_HOUR = 8

        const val ACTION_MORNING = "pl.dekrate.kofeino.tracker.action.REMINDER_MORNING"
        const val ACTION_REGULAR = "pl.dekrate.kofeino.tracker.action.REMINDER_REGULAR"
        const val ACTION_EVENING = "pl.dekrate.kofeino.tracker.action.REMINDER_EVENING"

        @Volatile
        internal var instance: CaffeineReminderManager? = null

        private val MORNING_VARIANTS = listOf(
            R.string.reminder_morning_title_1 to R.string.reminder_morning_body_1,
            R.string.reminder_morning_title_2 to R.string.reminder_morning_body_2,
            R.string.reminder_morning_title_3 to R.string.reminder_morning_body_3,
        )
        private val REGULAR_VARIANTS = listOf(
            R.string.reminder_regular_title_1 to R.string.reminder_regular_body_1,
            R.string.reminder_regular_title_2 to R.string.reminder_regular_body_2,
            R.string.reminder_regular_title_3 to R.string.reminder_regular_body_3,
        )
        private val EVENING_VARIANTS = listOf(
            R.string.reminder_evening_title_1 to R.string.reminder_evening_body_1,
            R.string.reminder_evening_title_2 to R.string.reminder_evening_body_2,
            R.string.reminder_evening_title_3 to R.string.reminder_evening_body_3,
        )

        /** Returns true if current local time is in the quiet window (22:00–08:00). */
        fun isNighttime(): Boolean {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
        }
    }

    init {
        instance = this
        createReminderChannel()
    }

    private fun createReminderChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID_REMINDERS)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
        Timber.tag(TAG).i("Reminder channel created")
    }

    fun scheduleMorning(enabled: Boolean) { scheduleAlarm(enabled, RC_MORNING, ACTION_MORNING, 8, 0) }
    fun scheduleRegular(enabled: Boolean) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REGULAR)
        val pi = PendingIntent.getBroadcast(context, RC_REGULAR, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (enabled) {
            val trigger = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, trigger, 3 * 60 * 60 * 1000L, pi)
        } else { alarm.cancel(pi) }
    }
    fun scheduleEvening(enabled: Boolean) { scheduleAlarm(enabled, RC_EVENING, ACTION_EVENING, 20, 0) }

    private fun scheduleAlarm(enabled: Boolean, reqCode: Int, action: String, hour: Int, min: Int) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        val pi = PendingIntent.getBroadcast(context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (enabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else { alarm.cancel(pi) }
    }

    /** If skipped due to nighttime, reschedule the regular reminder for 08:00 today/later. */
    private fun rescheduleRegularMorning() {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REGULAR)
        val pi = PendingIntent.getBroadcast(context, RC_REGULAR, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= NIGHT_START_HOUR) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, NIGHT_END_HOUR); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    @SuppressLint("MissingPermission")
    fun showReminder(reminderType: String) {
        // Skip regular reminders during nighttime (22:00–08:00) and reschedule
        if (reminderType == ACTION_REGULAR && isNighttime()) {
            Timber.tag(TAG).d("Nighttime — skipping regular reminder, rescheduling")
            rescheduleRegularMorning()
            return
        }

        val variants = when (reminderType) {
            ACTION_MORNING -> MORNING_VARIANTS
            ACTION_REGULAR -> REGULAR_VARIANTS
            ACTION_EVENING -> EVENING_VARIANTS
            else -> return
        }
        val (titleRes, bodyRes) = variants.random()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_caffeine)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderType.hashCode(), notification)
        Timber.tag(TAG).i("Reminder shown: $reminderType — variant #${variants.indexOf(titleRes to bodyRes)}")
    }
    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.i("CaffeineRcvr", "Receiver fired: $action")
            val mgr = CaffeineReminderManager.instance
            if (mgr != null) {
                mgr.showReminder(action)
            } else {
                Log.w("CaffeineRcvr", "instance null")
            }
        }
    }
}
