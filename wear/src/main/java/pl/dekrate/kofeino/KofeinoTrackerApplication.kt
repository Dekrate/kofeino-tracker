package pl.dekrate.kofeino

import android.app.Application
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.data.local.LanguagePreferences
import timber.log.Timber
import java.util.Locale

@HiltAndroidApp
class KofeinoTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        applySavedLocale()
    }

    /** Updates the Application-level locale so that @ApplicationContext
     *  injected into ViewModels reflects the current language. */
    fun refreshLocale() {
        val lang = LanguagePreferences.getLanguage(this)
        Locale.setDefault(Locale(lang))
        val config = Configuration(resources.configuration).apply {
            setLocale(Locale(lang))
        }
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun applySavedLocale() {
        val lang = LanguagePreferences.getLanguage(this)
        Locale.setDefault(Locale(lang))
    }
}
