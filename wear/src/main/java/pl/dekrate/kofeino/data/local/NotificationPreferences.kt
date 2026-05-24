package pl.dekrate.kofeino.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed notification toggle storage for watch module.
 *
 * Follows the same pattern as [CaffeinePreferences] for consistency.
 * Each reminder type (morning, regular, evening) has its own toggle.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isMorningEnabled(): Boolean = prefs.getBoolean(KEY_MORNING, DEFAULT_MORNING)
    fun setMorningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING, enabled).apply()
    }

    fun isRegularEnabled(): Boolean = prefs.getBoolean(KEY_REGULAR, DEFAULT_REGULAR)
    fun setRegularEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REGULAR, enabled).apply()
    }

    fun isEveningEnabled(): Boolean = prefs.getBoolean(KEY_EVENING, DEFAULT_EVENING)
    fun setEveningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EVENING, enabled).apply()
    }

    /** Reactive flow for morning toggle. */
    val morningFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MORNING) trySend(isMorningEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isMorningEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Reactive flow for regular toggle. */
    val regularFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_REGULAR) trySend(isRegularEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isRegularEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Reactive flow for evening toggle. */
    val eveningFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_EVENING) trySend(isEveningEnabled())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isEveningEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val FILE_NAME = "kofeino_notification_prefs"
        private const val KEY_MORNING = "notification_morning"
        private const val KEY_REGULAR = "notification_regular"
        private const val KEY_EVENING = "notification_evening"
        const val DEFAULT_MORNING = false
        const val DEFAULT_REGULAR = false
        const val DEFAULT_EVENING = false
    }
}
