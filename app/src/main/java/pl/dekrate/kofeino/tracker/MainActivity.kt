package pl.dekrate.kofeino.tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.navigation.AppNavHost
import javax.inject.Inject
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme
import pl.dekrate.kofeino.tracker.util.LocaleHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncStatusTracker: SyncStatusTracker

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissions()

        val themeMode = DataStorePreferences.getThemeMode(this)

        setContent {
            KofeinoTrackerPhoneTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController, syncStatusTracker = syncStatusTracker)
                }
            }
        }
    }

    /**
     * Requests notification permission required for the live updates feature.
     *
     * - [Manifest.permission.POST_NOTIFICATIONS] (Android 13+)
     *
     * The notification manager gracefully degrades if permission is denied.
     */
    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission result is handled gracefully — notification degrades silently
    }
}
