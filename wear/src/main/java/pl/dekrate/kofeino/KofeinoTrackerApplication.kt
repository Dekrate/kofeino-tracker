package pl.dekrate.kofeino

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.data.sync.WearableDataLayerManager
import pl.dekrate.kofeino.notification.WearNotificationObserver
import pl.dekrate.kofeino.presentation.util.LocaleHelper
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point for the Wear OS module.
 *
 * Uses the **Template Method** pattern via [attachBaseContext] to wrap the base
 * context with the user's saved language locale. This ensures that all
 * `@ApplicationContext` injections and Activity contexts reflect the chosen
 * language from startup, without relying on the deprecated
 * [android.content.res.Resources.updateConfiguration] API.
 */
@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    @Inject
    lateinit var notificationObserver: WearNotificationObserver

    @Inject
    lateinit var wearableDataLayerManager: WearableDataLayerManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        notificationObserver.start()
        wearableDataLayerManager.register()
    }

    override fun attachBaseContext(base: Context) {
        val lang = LanguagePreferences.getLanguage(base)
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }
}
