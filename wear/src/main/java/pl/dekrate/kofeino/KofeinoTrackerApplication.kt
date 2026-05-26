package pl.dekrate.kofeino

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.data.sync.WearableSyncService
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
 *
 * On [onCreate] the application starts [WearableSyncService] as a foreground
 * service, which manages the Wearable Data Layer listeners for cross-device
 * synchronisation.
 */
@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    @Inject
    lateinit var notificationObserver: WearNotificationObserver

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        notificationObserver.start()
        startSyncService()
    }

    /**
     * Starts [WearableSyncService] as a foreground service to manage the
     * Wearable Data Layer listener lifecycle.
     */
    private fun startSyncService() {
        val intent = Intent(this, WearableSyncService::class.java).apply {
            action = WearableSyncService.ACTION_START_SYNC
        }
        try {
            ContextCompat.startForegroundService(this, intent)
            Timber.d("WearableSyncService start requested")
        } catch (e: IllegalStateException) {
            Timber.e(e, "Failed to start WearableSyncService")
        }
    }

    override fun attachBaseContext(base: Context) {
        val lang = LanguagePreferences.getLanguage(base)
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }
}
