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
 * Synchronous language preference storage using SharedPreferences.
 * Must be readable from attachBaseContext (before Hilt is ready),
 * hence the companion factory method for static use.
 */
@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    /**
     * Emits the current language whenever it changes.
     * Uses SharedPreferences change listener under the hood.
     */
    val languageFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LANGUAGE) {
                trySend(getLanguage())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLanguage())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val FILE_NAME = "kofeino_language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        const val LANGUAGE_SYSTEM = ""
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_PL = "pl"
        const val DEFAULT_LANGUAGE = LANGUAGE_SYSTEM

        /** Static access for use before Hilt is initialized (attachBaseContext). */
        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        }
    }
}
