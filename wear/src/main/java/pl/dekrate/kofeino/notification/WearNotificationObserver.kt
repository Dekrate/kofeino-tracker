package pl.dekrate.kofeino.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.data.local.NotificationPreferences
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes [NotificationPreferences] toggles and wires them to
 * [CaffeineReminderManager] for actual alarm scheduling.
 *
 * Also manages a midnight reset alarm to clear daily state.
 */
@Singleton
class WearNotificationObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NotificationPreferences,
    private val reminderManager: CaffeineReminderManager
) {
    @Suppress("InjectDispatcher")
    private val defaultDispatcher = Dispatchers.Default

    @Volatile
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    fun start() {
        if (!isRunning.compareAndSet(false, true)) return
        Timber.tag(TAG).i("Starting notification observer")
        CaffeineReminderManager.instance = reminderManager

        // Defensive: cancel any lingering scope from a previous incomplete stop()
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

        observeReminderToggles()
        scheduleMidnightReset()
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
        Timber.tag(TAG).i("Observer stopped")
    }

    private fun observeReminderToggles() {
        scope.launch {
            combine(
                preferences.morningFlow,
                preferences.regularFlow,
                preferences.eveningFlow
            ) { morning, regular, evening ->
                Triple(morning, regular, evening)
            }.distinctUntilChanged()
                .retry { cause ->
                    Timber.tag(TAG).e(cause, "Error observing reminder toggles, retrying")
                    delay(RETRY_DELAY_MS)
                    true
                }
                .collect { (morning, regular, evening) ->
                Timber.tag(TAG).d(
                    "Reminder toggles — morning=%b regular=%b evening=%b",
                    morning, regular, evening
                )
                reminderManager.scheduleMorning(morning)
                reminderManager.scheduleRegular(regular)
                reminderManager.scheduleEvening(evening)
            }
        }
    }

    private fun scheduleMidnightReset() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MidnightReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val midnight = nextMidnightMillis()
        if (Build.VERSION.SDK_INT >= MIDNIGHT_SCHEDULE_API_LEVEL) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, midnight, pendingIntent)
        }
        Timber.tag(TAG).d("Midnight reset scheduled for $midnight")
    }

    private fun nextMidnightMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    class MidnightReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag(TAG).i("Midnight — resetting reminder state")
            Timber.tag(TAG).i("MidnightReceiver fired")
        }
    }

    private val isRunning = java.util.concurrent.atomic.AtomicBoolean(false)

    private companion object {
        private const val TAG = "WearNotifObs"
        private const val MIDNIGHT_SCHEDULE_API_LEVEL = 31
        private const val RETRY_DELAY_MS = 1000L
    }
}
