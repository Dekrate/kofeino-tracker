package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronous theme mode preference storage using SharedPreferences.
 * Follows the same pattern as [LanguagePreferences].
 */
@Singleton
class ThemePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    companion object {
        private const val FILE_NAME = "kofeino_theme_prefs"
        private const val KEY_THEME_MODE = "selected_theme_mode"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val DEFAULT_THEME_MODE = THEME_SYSTEM

        /** Static access for use before Hilt is initialized. */
        fun getThemeMode(context: Context): String {
            val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE
        }
    }
}
