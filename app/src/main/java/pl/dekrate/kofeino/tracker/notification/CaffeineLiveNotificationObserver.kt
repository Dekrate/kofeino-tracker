package pl.dekrate.kofeino.tracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HomeViewModel
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the caffeine repository for today's intake and pushes updates
 * to [CaffeineLiveNotificationManager].
 *
 * Respects [DataStorePreferences] notification toggles:
 * - `notification_live` — live tracking in notification shade
 * - `notification_morning` — morning reminder at 8:00
 * - `notification_regular` — regular reminder every 3h
 * - `notification_evening` — evening summary at 20:00
 */
@Singleton
class CaffeineLiveNotificationObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CaffeineRepository,
    private val notificationManager: CaffeineLiveNotificationManager,
    private val preferences: DataStorePreferences,
    private val reminderManager: CaffeineReminderManager
) {
    /** Injectable for testing — test dispatcher replaces Default. */
    @Volatile
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var observeJob: Job? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        Timber.tag(TAG).i("Starting notification observer")
        CaffeineLiveNotificationManager.instance = notificationManager
        CaffeineReminderManager.instance = reminderManager

        observeLiveToggle()
        observeReminderToggles()
        scheduleMidnightReset()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        observeJob?.cancel()
        observeJob = null
        repositoryJob?.cancel()
        repositoryJob = null
        reminderJob?.cancel()
        reminderJob = null
        dismiss()
        Timber.tag(TAG).i("Observer stopped")
    }

    fun dismiss() {
        notificationManager.dismiss()
    }

    // ── Observe preference changes ──

    private fun observeLiveToggle() {
        observeJob = scope.launch {
            preferences.observeNotificationLiveEnabled().collect { liveEnabled ->
                if (liveEnabled) {
                    Timber.tag(TAG).d("Live tracking enabled — starting observation")
                    notificationManager.resetDismissedFlag()
                    startObservingRepository()
                } else {
                    Timber.tag(TAG).d("Live tracking disabled — stopping observation")
                    stopObservingRepository()
                    notificationManager.dismiss()
                }
            }
        }
    }

    private var repositoryJob: Job? = null

    private fun startObservingRepository() {
        repositoryJob?.cancel()
        repositoryJob = scope.launch {
            repository.getTotalCaffeineForDate(todayStartMillis())
                .map { total -> total to isOverLimit(total) }
                .distinctUntilChanged()
                .catch { e -> Timber.tag(TAG).e(e, "Error observing caffeine total") }
                .collect { (total, _) ->
                    Timber.tag(TAG).d("Caffeine total updated: $total mg")
                    notificationManager.update(total, HomeViewModel.SAFE_LIMIT_MG)
                }
        }
    }

    private fun stopObservingRepository() {
        repositoryJob?.cancel()
        repositoryJob = null
    }

    // ── Reminder schedule observers ──

    private var reminderJob: Job? = null

    private fun observeReminderToggles() {
        reminderJob?.cancel()
        reminderJob = scope.launch {
            // Combine all three toggle flows — re-schedule when any changes
            combine(
                preferences.observeNotificationMorningEnabled(),
                preferences.observeNotificationRegularEnabled(),
                preferences.observeNotificationEveningEnabled()
            ) { morning, regular, evening ->
                Triple(morning, regular, evening)
            }.distinctUntilChanged().collect { (morning, regular, evening) ->
                Timber.tag(TAG).d("Reminder toggles — morning=%b regular=%b evening=%b", morning, regular, evening)
                reminderManager.scheduleMorning(morning)
                reminderManager.scheduleRegular(regular)
                reminderManager.scheduleEvening(evening)
            }
        }
    }

    // ── Midnight auto-reset ──

    private fun scheduleMidnightReset() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MidnightReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val midnight = nextMidnightMillis()
        if (Build.VERSION.SDK_INT >= 31) {
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

    private fun todayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isOverLimit(total: Int): Boolean = total > HomeViewModel.SAFE_LIMIT_MG

    class MidnightReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag(TAG).i("Midnight — resetting live notification")
            val manager = CaffeineLiveNotificationManager.instance
            if (manager != null) {
                manager.dismiss()
                manager.resetDismissedFlag()
            } else {
                Log.w(TAG, "MidnightReceiver: manager instance is null")
            }
        }
    }

    @Volatile
    private var isRunning: Boolean = false

    private companion object {
        private const val TAG = "CaffeineNotifObs"
    }
}
