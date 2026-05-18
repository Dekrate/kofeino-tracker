package pl.dekrate.kofeino.tracker

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.local.LanguagePreferences
import pl.dekrate.kofeino.tracker.util.LocaleHelper
import timber.log.Timber

/**
 * Application entry point.
 *
 * Uses the **Template Method** pattern via [attachBaseContext] to wrap the base
 * context with the user's saved language locale. This ensures that all
 * `@ApplicationContext` injections and Activity contexts reflect the chosen
 * language from startup, without relying on the deprecated
 * [android.content.res.Resources.updateConfiguration] API.
 *
 * When the user switches language in Settings, the Activity recreates and its
 * own [attachBaseContext] picks up the new locale. No Application-level
 * resource mutation is needed.
 */
@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun attachBaseContext(base: Context) {
        val lang = getLanguage(base)
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }

    companion object {
        /** Delegates to [DataStorePreferences] companion (reads SharedPreferences pre-Hilt). */
        fun getLanguage(context: Context): String = DataStorePreferences.getLanguage(context)

        /** Delegates to a new [LanguagePreferences] for pre-Hilt writes. */
        fun setLanguage(context: Context, lang: String) {
            LanguagePreferences(context).setLanguage(lang)
        }
    }
}
