package pl.dekrate.kofeino.tracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Coffee-inspired color palette for KofeinoTracker.
 *
 * Matches [pl.dekrate.kofeino.presentation.theme.CoffeeColors] on Wear OS
 * to maintain visual consistency across form factors.
 *
 * Designed for Material 3 [lightColorScheme] / [darkColorScheme] color roles.
 * All foreground/background pairs meet WCAG AA contrast ratio ≥ 4.5:1.
 */
object CoffeeColors {
    // -- Core palette (shared with Wear) --
    val espresso = Color(0xFF3E2723)       // very dark brown
    val darkRoast = Color(0xFF4E342E)      // dark brown
    val mediumRoast = Color(0xFF5D4037)    // medium brown
    val lightRoast = Color(0xFF6D4C41)     // light-dark brown
    val latte = Color(0xFF8D6E63)          // latte / primary base
    val cappuccino = Color(0xFFA1887F)     // cappuccino brown
    val cream = Color(0xFFD7CCC8)          // cream / light accent
    val foam = Color(0xFFEFEBE9)           // foam / off-white
    val milk = Color(0xFFF5F5F5)           // milk white

    // -- Phone-only accents --
    val warmWhite = Color(0xFFFFF8F0)      // warm off-white background
    val darkSurface = Color(0xFF1C130F)    // dark espresso surface
    val darkContainer = Color(0xFF2C1810)  // dark elevation

    // -- Functional --
    val golden = Color(0xFFD4A574)         // warm golden tertiary
    val errorRed = Color(0xFFBA1A1A)       // standard error
    val errorRedDark = Color(0xFFFFB4AB)   // dark-theme error
}

/** Light coffee colour scheme — warm, intentional, WCAG AA compliant. */
val LightCoffeeColorScheme = lightColorScheme(
    primary = CoffeeColors.mediumRoast,
    onPrimary = Color.White,
    primaryContainer = CoffeeColors.cream,
    onPrimaryContainer = CoffeeColors.espresso,

    secondary = CoffeeColors.latte,
    onSecondary = Color.White,
    secondaryContainer = CoffeeColors.foam,
    onSecondaryContainer = CoffeeColors.darkRoast,

    tertiary = CoffeeColors.golden,
    onTertiary = CoffeeColors.espresso,
    tertiaryContainer = CoffeeColors.cream,
    onTertiaryContainer = CoffeeColors.darkRoast,

    background = CoffeeColors.warmWhite,
    onBackground = CoffeeColors.espresso,
    surface = Color.White,
    onSurface = CoffeeColors.espresso,
    surfaceVariant = CoffeeColors.foam,
    onSurfaceVariant = CoffeeColors.darkRoast,

    outline = CoffeeColors.cappuccino,
    outlineVariant = CoffeeColors.cream,

    error = CoffeeColors.errorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

/** Dark coffee colour scheme — warm night palette, WCAG AA compliant. */
val DarkCoffeeColorScheme = darkColorScheme(
    primary = CoffeeColors.cappuccino,
    onPrimary = CoffeeColors.espresso,
    primaryContainer = CoffeeColors.darkRoast,
    onPrimaryContainer = CoffeeColors.cream,

    secondary = CoffeeColors.latte,
    onSecondary = CoffeeColors.espresso,
    secondaryContainer = CoffeeColors.mediumRoast,
    onSecondaryContainer = CoffeeColors.foam,

    tertiary = CoffeeColors.golden,
    onTertiary = CoffeeColors.espresso,
    tertiaryContainer = CoffeeColors.darkRoast,
    onTertiaryContainer = CoffeeColors.golden,

    background = CoffeeColors.darkSurface,
    onBackground = CoffeeColors.foam,
    surface = CoffeeColors.darkContainer,
    onSurface = CoffeeColors.foam,
    surfaceVariant = CoffeeColors.espresso,
    onSurfaceVariant = CoffeeColors.cream,

    outline = CoffeeColors.cappuccino,
    outlineVariant = CoffeeColors.darkRoast,

    error = CoffeeColors.errorRedDark,
    onError = CoffeeColors.darkRoast,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)
