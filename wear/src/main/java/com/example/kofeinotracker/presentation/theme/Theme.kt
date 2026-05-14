package com.example.kofeinotracker.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.compose.ui.platform.LocalContext

@Composable
fun KofeinoTrackerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = dynamicColorScheme(context) ?: error("Dynamic color scheme failed")


    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
