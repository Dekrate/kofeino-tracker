package pl.dekrate.kofeino.tracker

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val lang = getLanguage(this)
        Locale.setDefault(Locale.of(lang))
    }

    companion object {
        private const val PREFS_NAME = "kofeino_language_prefs"
        private const val KEY_LANGUAGE = "selected_language"

        fun getLanguage(context: Context): String {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        }

        fun setLanguage(context: Context, lang: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        }
    }
}
