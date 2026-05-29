package pl.dekrate.kofeino.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.dekrate.kofeino.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.data.sync.WearableSyncService
import pl.dekrate.kofeino.presentation.navigation.WearNavHost
import pl.dekrate.kofeino.presentation.screens.SyncStatusIndicator
import pl.dekrate.kofeino.presentation.theme.KofeinoTrackerTheme
import pl.dekrate.kofeino.presentation.util.LocaleHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncStatusTracker: SyncStatusTracker

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // Foreground service must be started from Activity (not Application)
        // to comply with Android 12+ foreground service restrictions.
        ContextCompat.startForegroundService(
            this,
            Intent(this, WearableSyncService::class.java)
        )

        setContent {
            KofeinoTrackerTheme {
                AppScaffold {
                    val navController = rememberSwipeDismissableNavController()

                    Box(modifier = Modifier.fillMaxSize()) {
                        WearNavHost(navController = navController)

                        // Sync status dot in the top-end corner
                        SyncStatusIndicator(
                            syncStatusTracker = syncStatusTracker,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}
