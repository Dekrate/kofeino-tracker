package pl.dekrate.kofeino

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import pl.dekrate.kofeino.data.local.LanguagePreferences
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
 * [WearableSyncService] is now started from [pl.dekrate.kofeino.presentation.MainActivity]
 * (not from Application.onCreate) to comply with Android 12+ foreground service
 * restrictions.
 */
@HiltAndroidApp
class KofeinoTrackerApplication : Application() {

    @Inject
    lateinit var notificationObserver: WearNotificationObserver

    override fun onCreate() {
        // Load the SQLCipher native library before any database access.
        // SQLCipher 4.14.0 does not auto-load the library in any static
        // initializer, so the app must load it explicitly. Without this,
        // SQLiteConnection.nativeOpen() throws UnsatisfiedLinkError.
        System.loadLibrary("sqlcipher")

        super.onCreate()
        Timber.plant(Timber.DebugTree())
        notificationObserver.start()
    }

    override fun attachBaseContext(base: Context) {
        val lang = LanguagePreferences.getLanguage(base)
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }
}
