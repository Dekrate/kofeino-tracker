package pl.dekrate.kofeino.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences

/**
 * KofeinoTracker phone theme with a warm, intentional coffee palette.
 *
 * Colour scheme is selected based on [themeMode] preference:
 * - [DataStorePreferences.THEME_SYSTEM] — follow device dark/light setting
 * - [DataStorePreferences.THEME_DARK]  — always dark
 * - [DataStorePreferences.THEME_LIGHT] — always light
 *
 * Both [LightCoffeeColorScheme] and [DarkCoffeeColorScheme] are derived from
 * the same [CoffeeColors] palette shared with Wear OS, ensuring visual
 * consistency across form factors.
 */
@Composable
fun KofeinoTrackerPhoneTheme(
    themeMode: String = DataStorePreferences.THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        DataStorePreferences.THEME_DARK -> true
        DataStorePreferences.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDarkTheme) DarkCoffeeColorScheme else LightCoffeeColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
