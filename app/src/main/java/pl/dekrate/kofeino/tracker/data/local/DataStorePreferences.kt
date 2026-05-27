package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.di.ApplicationScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Extension property — single DataStore instance per process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kofeino_settings")

/**
 * DataStore-backed preferences for language, theme, notifications, and caffeine limit profile.
 *
 * **Two-layer storage design**:
 * - [DataStore] is the canonical store (async, type-safe, modern).
 * - [SharedPreferences] mirror is required because [android.app.Application.attachBaseContext]
 *   runs before Hilt/DataStore are available. The static companion methods read from SP for
 *   that one-time init, while runtime code uses the DataStore-backed instance.
 *
 * **Migration**: On first DataStore read, values from SharedPreferences are migrated so no data
 * is lost. After migration, DataStore is the source of truth.
 *
 * **SOLID**:
 * - Single Responsibility: persistence concerns only
 * - Interface Segregation: callers depend on specific get/set methods, not on this class
 * - Dependency Inversion: ViewModels depend on this abstraction, not on storage details
 *
 * ## Usage
 * ```kotlin
 * // Runtime (post-Hilt):
 * @Inject lateinit var preferences: DataStorePreferences
 * val lang = preferences.getLanguage()          // Sync (cached, falls back to SP if not ready)
 * preferences.observeLanguage().collect { ... }  // Reactive
 * preferences.setLanguage("pl")                  // Async write
 * ```
 */
@Singleton
class DataStorePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // In-memory caches for synchronous access (avoids runBlocking on every read)
    @Volatile private var cachedLanguage: String = DEFAULT_LANGUAGE
    @Volatile private var cachedThemeMode: String = DEFAULT_THEME
    @Volatile private var cachedNotifLive: Boolean = DEFAULT_NOTIF_LIVE
    @Volatile private var cachedNotifMorning: Boolean = DEFAULT_NOTIF_MORNING
    @Volatile private var cachedNotifRegular: Boolean = DEFAULT_NOTIF_REGULAR
    @Volatile private var cachedNotifEvening: Boolean = DEFAULT_NOTIF_EVENING
    @Volatile private var cachedProfile: String = DEFAULT_CAFFEINE_PROFILE
    @Volatile private var cachedCustomLimit: Int = DEFAULT_CUSTOM_CAFFEINE_LIMIT
    @Volatile private var initialized = false

    /** Eagerly warm the cache from DataStore on construction. */
    init {
        applicationScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Timber.w(e, "DataStorePreferences: unhandled exception in warm-up coroutine")
        }) {
            try {
                val prefs = dataStore.data.first()
                cachedLanguage = prefs[LANGUAGE_KEY] ?: migrateLanguageFromSp()
                cachedThemeMode = prefs[THEME_KEY] ?: migrateThemeFromSp()
                cachedNotifLive = prefs[NOTIF_LIVE_KEY] ?: DEFAULT_NOTIF_LIVE
                cachedNotifMorning = prefs[NOTIF_MORNING_KEY] ?: DEFAULT_NOTIF_MORNING
                cachedNotifRegular = prefs[NOTIF_REGULAR_KEY] ?: DEFAULT_NOTIF_REGULAR
                cachedNotifEvening = prefs[NOTIF_EVENING_KEY] ?: DEFAULT_NOTIF_EVENING
                cachedProfile = prefs[CAFFEINE_PROFILE_KEY] ?: DEFAULT_CAFFEINE_PROFILE
                cachedCustomLimit = prefs[CUSTOM_CAFFEINE_LIMIT_KEY] ?: DEFAULT_CUSTOM_CAFFEINE_LIMIT
                initialized = true
                Timber.d(
                    "DataStorePreferences initialized — lang=%s theme=%s notif=%b,%b,%b,%b profile=%s customLimit=%d",
                    cachedLanguage, cachedThemeMode,
                    cachedNotifLive, cachedNotifMorning, cachedNotifRegular, cachedNotifEvening,
                    cachedProfile, cachedCustomLimit
                )
            } catch (e: Exception) {
                // Fallback to SharedPreferences
                cachedLanguage = LanguagePreferences.getLanguage(context)
                cachedThemeMode = ThemePreferences.getThemeMode(context)
                initialized = true
                Timber.w(e, "DataStore init failed, using SharedPreferences fallback")
            }
        }
    }

    // ===== Language =====

    /**
     * Synchronous read — backed by in-memory cache.
     * Falls back to SharedPreferences if the DataStore warm-up hasn't completed yet.
     */
    fun getLanguage(): String {
        if (!initialized) return LanguagePreferences.getLanguage(context)
        return cachedLanguage
    }

    /** Reactive stream — always reads from DataStore (source of truth). */
    fun observeLanguage(): Flow<String> = dataStore.data.map { settings ->
        settings[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
    }

    /** Persist language preference. */
    suspend fun setLanguage(lang: String) {
        dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = lang
        }
        cachedLanguage = lang
        initialized = true
        // Mirror to SharedPreferences for attachBaseContext
        LanguagePreferences(context).setLanguage(lang)
        Timber.d("Language set to: %s", lang)
    }

    // ===== Theme =====

    /**
     * Synchronous read — backed by in-memory cache.
     * Falls back to SharedPreferences if the DataStore warm-up hasn't completed yet.
     */
    fun getThemeMode(): String {
        if (!initialized) return ThemePreferences.getThemeMode(context)
        return cachedThemeMode
    }

    /** Reactive stream — always reads from DataStore (source of truth). */
    fun observeThemeMode(): Flow<String> = dataStore.data.map { settings ->
        settings[THEME_KEY] ?: DEFAULT_THEME
    }

    /** Persist theme mode. */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { settings ->
            settings[THEME_KEY] = mode
        }
        cachedThemeMode = mode
        initialized = true
        // Mirror to SharedPreferences for attachBaseContext
        ThemePreferences(context).setThemeMode(mode)
        Timber.d("Theme mode set to: %s", mode)
    }

    // ===== Notification toggles =====

    fun isNotificationLiveEnabled(): Boolean = cachedNotifLive
    fun observeNotificationLiveEnabled(): Flow<Boolean> = dataStore.data.map { s ->
        s[NOTIF_LIVE_KEY] ?: DEFAULT_NOTIF_LIVE
    }
    suspend fun setNotificationLiveEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIF_LIVE_KEY] = enabled }
        cachedNotifLive = enabled
    }

    fun isNotificationMorningEnabled(): Boolean = cachedNotifMorning
    fun observeNotificationMorningEnabled(): Flow<Boolean> = dataStore.data.map { s ->
        s[NOTIF_MORNING_KEY] ?: DEFAULT_NOTIF_MORNING
    }
    suspend fun setNotificationMorningEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIF_MORNING_KEY] = enabled }
        cachedNotifMorning = enabled
    }

    fun isNotificationRegularEnabled(): Boolean = cachedNotifRegular
    fun observeNotificationRegularEnabled(): Flow<Boolean> = dataStore.data.map { s ->
        s[NOTIF_REGULAR_KEY] ?: DEFAULT_NOTIF_REGULAR
    }
    suspend fun setNotificationRegularEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIF_REGULAR_KEY] = enabled }
        cachedNotifRegular = enabled
    }

    fun isNotificationEveningEnabled(): Boolean = cachedNotifEvening
    fun observeNotificationEveningEnabled(): Flow<Boolean> = dataStore.data.map { s ->
        s[NOTIF_EVENING_KEY] ?: DEFAULT_NOTIF_EVENING
    }
    suspend fun setNotificationEveningEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIF_EVENING_KEY] = enabled }
        cachedNotifEvening = enabled
    }

    // ===== Caffeine Limit Profile =====

    /**
     * Synchronous read of current caffeine limit profile.
     * Falls back to [CaffeineLimitProfile.ADULT] if cache is not yet initialized.
     */
    fun getCaffeineProfile(): CaffeineLimitProfile {
        val name = if (!initialized) DEFAULT_CAFFEINE_PROFILE else cachedProfile
        return try {
            CaffeineLimitProfile.valueOf(name)
        } catch (_: IllegalArgumentException) {
            CaffeineLimitProfile.ADULT
        }
    }

    /** Reactive stream of caffeine profile changes. */
    fun observeCaffeineProfile(): Flow<CaffeineLimitProfile> = dataStore.data.map { settings ->
        val name = settings[CAFFEINE_PROFILE_KEY] ?: DEFAULT_CAFFEINE_PROFILE
        try {
            CaffeineLimitProfile.valueOf(name)
        } catch (_: IllegalArgumentException) {
            CaffeineLimitProfile.ADULT
        }
    }

    /** Persist caffeine profile. */
    suspend fun setCaffeineProfile(profile: CaffeineLimitProfile) {
        dataStore.edit { settings ->
            settings[CAFFEINE_PROFILE_KEY] = profile.name
        }
        cachedProfile = profile.name
        initialized = true
        Timber.d("Caffeine profile set to: %s", profile.name)
    }

    /**
     * Synchronous read of custom caffeine limit.
     * Defaults to 400 if not set.
     */
    fun getCustomCaffeineLimit(): Int {
        val limit = if (!initialized) DEFAULT_CUSTOM_CAFFEINE_LIMIT else cachedCustomLimit
        return limit.coerceIn(MIN_CUSTOM_LIMIT, MAX_CUSTOM_LIMIT)
    }

    /** Reactive stream of custom caffeine limit changes. */
    fun observeCustomCaffeineLimit(): Flow<Int> = dataStore.data.map { settings ->
        (settings[CUSTOM_CAFFEINE_LIMIT_KEY] ?: DEFAULT_CUSTOM_CAFFEINE_LIMIT)
            .coerceIn(MIN_CUSTOM_LIMIT, MAX_CUSTOM_LIMIT)
    }

    /** Persist custom caffeine limit (coerced to valid range). */
    suspend fun setCustomCaffeineLimit(mg: Int) {
        val clamped = mg.coerceIn(MIN_CUSTOM_LIMIT, MAX_CUSTOM_LIMIT)
        dataStore.edit { settings ->
            settings[CUSTOM_CAFFEINE_LIMIT_KEY] = clamped
        }
        cachedCustomLimit = clamped
        initialized = true
        Timber.d("Custom caffeine limit set to: %d mg", clamped)
    }

    /**
     * Convenient synchronous read — returns the effective daily caffeine limit
     * based on the selected profile (or custom limit for CUSTOM profile).
     */
    fun getCaffeineLimitMg(): Int {
        val profile = getCaffeineProfile()
        return profile.limitMg ?: getCustomCaffeineLimit()
    }

    /**
     * Reactive stream of the effective daily caffeine limit.
     * Combines profile and custom limit flows so any change in either triggers an emission.
     */
    fun observeCaffeineLimitMg(): Flow<Int> = combine(
        observeCaffeineProfile(),
        observeCustomCaffeineLimit()
    ) { profile, customLimit ->
        profile.limitMg ?: customLimit
    }

    /** Returns true once the cache has been populated. */
    fun isReady(): Boolean = initialized

    // ===== Migration =====

    private suspend fun migrateLanguageFromSp(): String {
        val old = LanguagePreferences.getLanguage(context)
        if (old != DEFAULT_LANGUAGE) {
            dataStore.edit { settings ->
                settings[LANGUAGE_KEY] = old
            }
            Timber.d("Migrated language '%s' from SharedPreferences \u2192 DataStore", old)
        }
        return old
    }

    private suspend fun migrateThemeFromSp(): String {
        val old = ThemePreferences.getThemeMode(context)
        if (old != DEFAULT_THEME) {
            dataStore.edit { settings ->
                settings[THEME_KEY] = old
            }
            Timber.d("Migrated theme '%s' from SharedPreferences \u2192 DataStore", old)
        }
        return old
    }

    // ===== Static access (for attachBaseContext — pre-Hilt) =====

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
        private val THEME_KEY = stringPreferencesKey("selected_theme_mode")
        private val NOTIF_LIVE_KEY = booleanPreferencesKey("notification_live")
        private val NOTIF_MORNING_KEY = booleanPreferencesKey("notification_morning")
        private val NOTIF_REGULAR_KEY = booleanPreferencesKey("notification_regular")
        private val NOTIF_EVENING_KEY = booleanPreferencesKey("notification_evening")
        private val CAFFEINE_PROFILE_KEY = stringPreferencesKey("caffeine_profile")
        private val CUSTOM_CAFFEINE_LIMIT_KEY = intPreferencesKey("custom_caffeine_limit")

        const val LANGUAGE_SYSTEM = LanguagePreferences.LANGUAGE_SYSTEM
        const val LANGUAGE_PL = LanguagePreferences.LANGUAGE_PL
        const val LANGUAGE_EN = LanguagePreferences.LANGUAGE_EN
        const val DEFAULT_LANGUAGE = LanguagePreferences.DEFAULT_LANGUAGE

        const val THEME_SYSTEM = ThemePreferences.THEME_SYSTEM
        const val THEME_LIGHT = ThemePreferences.THEME_LIGHT
        const val THEME_DARK = ThemePreferences.THEME_DARK
        const val DEFAULT_THEME = ThemePreferences.DEFAULT_THEME_MODE

        const val DEFAULT_NOTIF_LIVE = true
        const val DEFAULT_NOTIF_MORNING = false
        const val DEFAULT_NOTIF_REGULAR = false
        const val DEFAULT_NOTIF_EVENING = false

        const val DEFAULT_CAFFEINE_PROFILE = "ADULT"
        const val DEFAULT_CUSTOM_CAFFEINE_LIMIT = 400
        const val MIN_CUSTOM_LIMIT = 25
        const val MAX_CUSTOM_LIMIT = 2000

        /**
         * Synchronous read for [android.app.Application.attachBaseContext].
         * Uses SharedPreferences because DataStore is not yet available.
         */
        fun getLanguage(context: Context): String = LanguagePreferences.getLanguage(context)

        /**
         * Synchronous read for [android.app.Application.attachBaseContext].
         */
        fun getThemeMode(context: Context): String = ThemePreferences.getThemeMode(context)
    }
}
