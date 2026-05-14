package pl.dekrate.kofeino

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KofeinoTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (pl.dekrate.kofeino.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
