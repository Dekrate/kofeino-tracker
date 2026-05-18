package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Extension property — single DataStore instance per process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kofeino_settings")

/**
 * DataStore-backed preferences for language and theme.
 *
 * **Why two layers?**
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
 * val lang = preferences.getLanguage()          // Sync (cached)
 * preferences.observeLanguage().collect { ... }  // Reactive
 * preferences.setLanguage("pl")                  // Async write
 *
 * // Pre-Hilt (attachBaseContext):
 * DataStorePreferences.getLanguage(context)
 * DataStorePreferences.getThemeMode(context)
 * ```
 */
@Singleton
class DataStorePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // In-memory caches for synchronous access (avoids runBlocking on every read)
    @Volatile private var cachedLanguage: String = DEFAULT_LANGUAGE
    @Volatile private var cachedThemeMode: String = DEFAULT_THEME
    @Volatile private var initialized = false

    /** Eagerly warm the cache from DataStore on construction. */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = dataStore.data.first()
                cachedLanguage = prefs[LANGUAGE_KEY] ?: migrateLanguageFromSp()
                cachedThemeMode = prefs[THEME_KEY] ?: migrateThemeFromSp()
                initialized = true
                Timber.d("DataStorePreferences initialized — lang=%s theme=%s", cachedLanguage, cachedThemeMode)
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

    /** Synchronous read — backed by in-memory cache. */
    fun getLanguage(): String = cachedLanguage

    /** Reactive stream. */
    fun observeLanguage(): Flow<String> = dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
    }

    /** Persist language preference. */
    suspend fun setLanguage(lang: String) {
        dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = lang
        }
        cachedLanguage = lang
        // Mirror to SharedPreferences for attachBaseContext
        LanguagePreferences(context).setLanguage(lang)
        Timber.d("Language set to: %s", lang)
    }

    // ===== Theme =====

    /** Synchronous read — backed by in-memory cache. */
    fun getThemeMode(): String = cachedThemeMode

    /** Reactive stream. */
    fun observeThemeMode(): Flow<String> = dataStore.data.map { settings ->
        settings[THEME_KEY] ?: DEFAULT_THEME
    }

    /** Persist theme mode. */
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { settings ->
            settings[THEME_KEY] = mode
        }
        cachedThemeMode = mode
        // Mirror to SharedPreferences for attachBaseContext
        ThemePreferences(context).setThemeMode(mode)
        Timber.d("Theme mode set to: %s", mode)
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
            Timber.d("Migrated language '%s' from SharedPreferences → DataStore", old)
        }
        return old
    }

    private suspend fun migrateThemeFromSp(): String {
        val old = ThemePreferences.getThemeMode(context)
        if (old != DEFAULT_THEME) {
            dataStore.edit { settings ->
                settings[THEME_KEY] = old
            }
            Timber.d("Migrated theme '%s' from SharedPreferences → DataStore", old)
        }
        return old
    }

    // ===== Static access (for attachBaseContext — pre-Hilt) =====

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
        private val THEME_KEY = stringPreferencesKey("selected_theme_mode")

        const val LANGUAGE_SYSTEM = LanguagePreferences.LANGUAGE_SYSTEM
        const val LANGUAGE_PL = LanguagePreferences.LANGUAGE_PL
        const val LANGUAGE_EN = LanguagePreferences.LANGUAGE_EN
        const val DEFAULT_LANGUAGE = LanguagePreferences.DEFAULT_LANGUAGE

        const val THEME_SYSTEM = ThemePreferences.THEME_SYSTEM
        const val THEME_LIGHT = ThemePreferences.THEME_LIGHT
        const val THEME_DARK = ThemePreferences.THEME_DARK
        const val DEFAULT_THEME = ThemePreferences.DEFAULT_THEME_MODE

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
