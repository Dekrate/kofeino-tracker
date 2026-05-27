package pl.dekrate.kofeino.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.pow

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

// ── WCAG 2.1 specification constants ────────────────────────────────────────

private const val WcagSrgbThreshold = 0.04045
private const val WcagSrgbDivisor = 12.92
private const val WcagSrgbOffset = 0.055
private const val WcagSrgbBase = 1.055
private const val WcagSrgbGamma = 2.4
private const val WcagLumRed = 0.2126
private const val WcagLumGreen = 0.7152
private const val WcagLumBlue = 0.0722
private const val WcagContrastOffset = 0.05

// ── Accessibility / contrast utilities ──────────────────────────────────────

/**
 * Minimum contrast ratio for normal-sized text per WCAG 2.1 AA.
 *
 * @see [WCAG 2.1 Contrast Ratio](https://www.w3.org/TR/WCAG21/#contrast-minimum)
 */
internal const val MinContrastRatioNormal = 4.5

/**
 * Minimum contrast ratio for large-sized text (≥18pt or ≥14pt bold) per WCAG 2.1 AA.
 *
 * @see [WCAG 2.1 Contrast Ratio](https://www.w3.org/TR/WCAG21/#contrast-minimum)
 */
internal const val MinContrastRatioLarge = 3.0

/**
 * Returns the WCAG 2.1 relative luminance of this color.
 *
 * Formula per [WCAG 2.1](https://www.w3.org/TR/WCAG21/#dfn-relative-luminance).
 * Values range from 0 (pure black) to 1 (pure white).
 *
 * Relative luminance is used as the basis for calculating contrast ratios.
 */
fun Color.luminance(): Double {
    fun linearize(c: Float): Double {
        val srgb = c.toDouble()
        return if (srgb <= WcagSrgbThreshold) srgb / WcagSrgbDivisor
        else ((srgb + WcagSrgbOffset) / WcagSrgbBase).pow(WcagSrgbGamma)
    }
    return WcagLumRed * linearize(red) + WcagLumGreen * linearize(green) + WcagLumBlue * linearize(blue)
}

/**
 * Returns the WCAG 2.1 contrast ratio between this color and [other].
 *
 * Ratio = (L1 + 0.05) / (L2 + 0.05) where L1 is the lighter luminance.
 * WCAG 2.1 AA requires 4.5:1 for normal text, 3:1 for large text.
 *
 * @see [Understanding Contrast Ratio](https://www.w3.org/TR/WCAG21/#contrast-minimum)
 */
fun Color.contrastRatio(other: Color): Double {
    val l1 = luminance()
    val l2 = other.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + WcagContrastOffset) / (darker + WcagContrastOffset)
}

/**
 * Returns this color adjusted (lightened or darkened) to meet the minimum
 * [contrastRatio] against [onBackground].
 *
 * If this color already meets the requirement it is returned unchanged.
 * Otherwise the color is blended towards white or black in five steps until
 * the minimum contrast ratio is achieved. As a last resort pure white or black
 * is returned.
 *
 * Usage:
 * ```
 * val readableColor = someColor.ensureContrast(onBackground = backgroundColor)
 * ```
 */
@Suppress("MagicNumber")
fun Color.ensureContrast(
    onBackground: Color,
    minRatio: Double = MinContrastRatioNormal
): Color {
    if (contrastRatio(onBackground) >= minRatio) return this

    // Lighten towards white in 5 steps
    var result = this
    for (fraction in listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.95f)) {
        result = copy(
            red = red * (1f - fraction) + fraction,
            green = green * (1f - fraction) + fraction,
            blue = blue * (1f - fraction) + fraction
        )
        if (result.contrastRatio(onBackground) >= minRatio) return result
    }

    // Darken towards black in 5 steps
    result = this
    for (fraction in listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.95f)) {
        result = copy(
            red = red * (1f - fraction),
            green = green * (1f - fraction),
            blue = blue * (1f - fraction)
        )
        if (result.contrastRatio(onBackground) >= minRatio) return result
    }

    // Fallback — return whichever extreme (white or black) gives better contrast
    return if (Color.White.contrastRatio(onBackground) >= Color.Black.contrastRatio(onBackground))
        Color.White else Color.Black
}

@Composable
fun KofeinoTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoffeeColorScheme,
        content = content
    )
}
