package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous language preference storage using SharedPreferences.
 * Must be readable from [android.app.Application.attachBaseContext]
 * (before Hilt is ready), hence the companion factory method for static use.
 */
@Singleton
class LanguagePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    companion object {
        private const val FILE_NAME = "kofeino_language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        const val LANGUAGE_SYSTEM = ""
        const val LANGUAGE_PL = "pl"
        const val LANGUAGE_EN = "en"
        const val DEFAULT_LANGUAGE = LANGUAGE_SYSTEM

        /** Static access for use before Hilt is initialized (attachBaseContext). */
        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        }
    }
}
