package com.example.kofeinotracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.kofeinotracker.presentation.navigation.WearNavHost
import com.example.kofeinotracker.presentation.theme.KofeinoTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
