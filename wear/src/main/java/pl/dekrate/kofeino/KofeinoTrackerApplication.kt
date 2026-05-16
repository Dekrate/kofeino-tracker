package pl.dekrate.kofeino

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.data.local.LanguagePreferences
import timber.log.Timber
import java.util.Locale

@HiltAndroidApp
class KofeinoTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (pl.dekrate.kofeino.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Apply saved language as default locale
        val lang = LanguagePreferences.getLanguage(this)
        Locale.setDefault(Locale(lang))
    }
}
