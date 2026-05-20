package pl.dekrate.kofeino.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.dekrate.kofeino.domain.model.CaffeineLimitProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous caffeine limit preference storage using SharedPreferences.
 *
 * Stores the selected [CaffeineLimitProfile] and (if CUSTOM) the user-defined limit.
 */
@Singleton
class CaffeinePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Returns the currently selected profile (default: ADULT). */
    fun getProfile(): CaffeineLimitProfile {
        val name = prefs.getString(KEY_PROFILE, null) ?: return CaffeineLimitProfile.ADULT
        return try {
            CaffeineLimitProfile.valueOf(name)
        } catch (_: IllegalArgumentException) {
            CaffeineLimitProfile.ADULT
        }
    }

    /** Persists the selected profile. */
    fun setProfile(profile: CaffeineLimitProfile) {
        prefs.edit().putString(KEY_PROFILE, profile.name).apply()
    }

    /** Returns the custom limit in mg (default: 400). Only meaningful for [CaffeineLimitProfile.CUSTOM]. */
    fun getCustomLimit(): Int = prefs
        .getInt(KEY_CUSTOM_LIMIT, DEFAULT_CUSTOM_LIMIT)
        .coerceIn(MIN_CUSTOM_LIMIT, MAX_CUSTOM_LIMIT)

    /** Persists the custom limit. */
    fun setCustomLimit(mg: Int) {
        prefs.edit().putInt(KEY_CUSTOM_LIMIT, mg.coerceIn(MIN_CUSTOM_LIMIT, MAX_CUSTOM_LIMIT)).apply()
    }

    /**
     * Convenience method: returns the effective daily limit in mg based on
     * the current profile and custom limit.
     */
    fun getLimitMg(): Int {
        val profile = getProfile()
        return profile.limitMg ?: getCustomLimit()
    }

    /**
     * Emits the effective daily limit whenever the profile or custom limit changes.
     * Uses SharedPreferences change listener under the hood.
     */
    val limitFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PROFILE || key == KEY_CUSTOM_LIMIT) {
                trySend(getLimitMg())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLimitMg())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val FILE_NAME = "kofeino_caffeine_prefs"
        private const val KEY_PROFILE = "caffeine_profile"
        private const val KEY_CUSTOM_LIMIT = "custom_caffeine_limit"
        const val DEFAULT_CUSTOM_LIMIT = 400
        const val MIN_CUSTOM_LIMIT = 25
        const val MAX_CUSTOM_LIMIT = 2000
    }
}
