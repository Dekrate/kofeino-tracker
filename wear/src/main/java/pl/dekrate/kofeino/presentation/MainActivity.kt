package pl.dekrate.kofeino.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.dekrate.kofeino.presentation.navigation.WearNavHost
import pl.dekrate.kofeino.presentation.theme.KofeinoTrackerTheme
import pl.dekrate.kofeino.presentation.util.LocaleHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            KofeinoTrackerTheme {
                AppScaffold {
                    val navController = rememberSwipeDismissableNavController()
                    WearNavHost(navController = navController)
                }
            }
        }
    }
}
