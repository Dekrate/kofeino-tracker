package pl.dekrate.kofeino.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Coffee / brown color palette
object CoffeeColors {
    val espresso = Color(0xFF3E2723)       // very dark brown (coffee fill)
    val darkRoast = Color(0xFF4E342E)       // dark brown
    val mediumRoast = Color(0xFF5D4037)     // medium brown
    val lightRoast = Color(0xFF6D4C41)      // light-dark brown
    val latte = Color(0xFF8D6E63)           // latte brown (primary)
    val cappuccino = Color(0xFFA1887F)      // cappuccino brown
    val cream = Color(0xFFD7CCC8)           // cream / light brown
    val foam = Color(0xFFEFEBE9)            // foam / off-white
    val milk = Color(0xFFF5F5F5)            // milk white

    val errorRed = Color(0xFFFF6B6B)        // soft red for error
    val errorContainer = Color(0xFF3E2723)  // dark container for error
}

val CoffeeColorScheme = ColorScheme(
    primary = CoffeeColors.latte,
    primaryDim = CoffeeColors.mediumRoast,
    primaryContainer = CoffeeColors.cream,
    onPrimary = Color.White,
    onPrimaryContainer = CoffeeColors.espresso,

    secondary = CoffeeColors.cappuccino,
    secondaryDim = CoffeeColors.lightRoast,
    secondaryContainer = CoffeeColors.foam,
    onSecondary = Color.White,
    onSecondaryContainer = CoffeeColors.espresso,

    tertiary = CoffeeColors.cream,
    tertiaryDim = CoffeeColors.cappuccino,
    tertiaryContainer = CoffeeColors.foam,
    onTertiary = CoffeeColors.espresso,
    onTertiaryContainer = CoffeeColors.darkRoast,

    surfaceContainerLow = Color.Black,
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF252525),
    onSurface = CoffeeColors.foam,
    onSurfaceVariant = CoffeeColors.cream,

    background = Color.Black,
    onBackground = CoffeeColors.foam,

    outline = CoffeeColors.cappuccino,
    outlineVariant = CoffeeColors.lightRoast,

    error = CoffeeColors.errorRed,
    errorDim = CoffeeColors.errorRed.copy(alpha = 0.7f),
    errorContainer = CoffeeColors.errorContainer,
    onError = Color.White,
    onErrorContainer = CoffeeColors.foam
)

@Composable
fun KofeinoTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoffeeColorScheme,
        content = content
    )
}
