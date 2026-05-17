package pl.dekrate.kofeino.tracker

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.tracker.data.local.LanguagePreferences
import java.util.Locale

@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLocale()
    }

    /**
     * Updates the Application-level locale so that @ApplicationContext
     * injected into ViewModels reflects the current language.
     */
    fun refreshLocale() {
        applyLocale(getLanguage(this))
    }

    private fun applySavedLocale() {
        applyLocale(getLanguage(this))
    }

    /** Shared helper that sets both the default Locale and the resource Configuration. */
    private fun applyLocale(lang: String) {
        @Suppress("DEPRECATION")
        val locale = if (lang.isNotEmpty()) {
            Locale(lang)
        } else {
            // System default — use the device's system locale
            Resources.getSystem().configuration.locales[0]
        }
        @Suppress("DEPRECATION")
        Locale.setDefault(locale)
        @Suppress("DEPRECATION")
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
        }
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object {
        /** Delegates to [LanguagePreferences] companion for consistency. */
        fun getLanguage(context: Context): String = LanguagePreferences.getLanguage(context)

        /** Delegates to a new [LanguagePreferences] for consistency. */
        fun setLanguage(context: Context, lang: String) {
            LanguagePreferences(context).setLanguage(lang)
        }
    }
}
