package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.dekrate.kofeino.tracker.R

/**
 * Coffee cup indicator for the phone:
 * - Larger canvas (180dp) vs watch (120dp)
 * - Animated fill based on daily caffeine progress
 * - Circular progress ring around the cup
 * - Steam animation when limit exceeded
 * - Overlaid caffeine count
 */
@Composable
fun CoffeeCupIndicator(
    total: Int,
    progress: Float,
    exceeded: Boolean,
    safeLimitMg: Int = 400,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "coffeeProgress"
    )

    val steamTransition = rememberInfiniteTransition(label = "steam")
    val steamPhase by steamTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamPhase"
    )

    val cupColor = if (exceeded) CoffeeErrorRed else CoffeeLatte
    val coffeeFill = when {
        exceeded -> CoffeeErrorRed.copy(alpha = 0.5f)
        animatedProgress > 0.75f -> CoffeeMediumRoast
        animatedProgress > 0.4f -> CoffeeLightRoast
        else -> CoffeeDarkRoast
    }
    val ringColor = when {
        exceeded -> MaterialTheme.colorScheme.error
        animatedProgress >= 1f -> MaterialTheme.colorScheme.primary
        animatedProgress > 0.6f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val contentDesc = if (exceeded) {
        "${stringResource(R.string.limit_exceeded)}: $total mg"
    } else {
        "$total mg ${stringResource(R.string.safe_limit)}"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coffee cup
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    // Progress ring behind cup
                    drawProgressRing(
                        progress = animatedProgress,
                        ringColor = ringColor,
                        trackColor = ringTrackColor,
                        strokeWidth = 6.dp.toPx()
                    )

                    // Coffee cup
                    drawCoffeeCup(
                        progress = animatedProgress,
                        exceeded = exceeded,
                        cupColor = cupColor,
                        coffeeFill = coffeeFill,
                        steamPhase = steamPhase
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            // Caffeine stats column alongside the cup
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (exceeded) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "mg",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "/ $safeLimitMg mg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (exceeded) {
            Text(
                text = stringResource(R.string.limit_exceeded),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}% ${stringResource(R.string.safe_limit)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}

// Phone-specific color palette for the cup
private val CoffeeLatte = Color(0xFFC8A67A)
private val CoffeeDarkRoast = Color(0xFF3E2723)
private val CoffeeMediumRoast = Color(0xFF6D4C41)
private val CoffeeLightRoast = Color(0xFF8D6E63)
private val CoffeeErrorRed = Color(0xFFE53935)
private val CoffeeFoam = Color(0xFFFFF8E1)

private fun DrawScope.drawProgressRing(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    strokeWidth: Float
) {
    val diameter = size.minDimension - strokeWidth
    val topLeft = Offset(
        (size.width - diameter) / 2f,
        (size.height - diameter) / 2f
    )

    // Track
    drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = Size(diameter, diameter),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress
    drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = 360f * progress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = topLeft,
        size = Size(diameter, diameter),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCoffeeCup(
    progress: Float,
    exceeded: Boolean,
    cupColor: Color,
    coffeeFill: Color,
    steamPhase: Float = 0f
) {
    val w = size.width
    val h = size.height
    val strokeW = 3.5.dp.toPx()

    // Cup body dimensions (centered, leaving room for ring)
    val margin = 18.dp.toPx()
    val bodyLeft = margin
    val bodyRight = w - margin
    val bodyTop = h * 0.22f
    val bodyBottom = h * 0.82f
    val bodyWidth = bodyRight - bodyLeft
    val bodyHeight = bodyBottom - bodyTop

    // Coffee fill region (leave room at bottom for flat base)
    val fillableHeight = bodyHeight * 0.88f
    val coffeeTop = bodyBottom - fillableHeight * progress

    // Handle
    val handleCy = bodyTop + bodyHeight * 0.42f
    val handleRadius = bodyWidth * 0.18f

    // Build cup body path (rounded rect, slightly tapered)
    val cupPath = Path().apply {
        val taper = bodyWidth * 0.08f
        val topLeft = Offset(bodyLeft, bodyTop)
        val topRight = Offset(bodyRight, bodyTop)
        val bottomLeft = Offset(bodyLeft + taper, bodyBottom)
        val bottomRight = Offset(bodyRight - taper, bodyBottom)

        moveTo(topLeft.x, topLeft.y + h * 0.02f)
        lineTo(topRight.x, topRight.y + h * 0.02f)
        quadraticTo(
            topRight.x + bodyWidth * 0.02f, topRight.y + bodyHeight * 0.5f,
            bottomRight.x, bottomRight.y
        )
        quadraticTo(
            (bottomRight.x + bottomLeft.x) / 2f, bottomRight.y + h * 0.04f,
            bottomLeft.x, bottomLeft.y
        )
        quadraticTo(
            topLeft.x - bodyWidth * 0.02f, topLeft.y + bodyHeight * 0.5f,
            topLeft.x, topLeft.y + h * 0.02f
        )
        close()
    }

    // Coffee fill (clipped to cup body)
    clipPath(cupPath) {
        drawRect(
            color = coffeeFill,
            topLeft = Offset(bodyLeft, coffeeTop),
            size = Size(bodyWidth, bodyBottom - coffeeTop)
        )

        // Crema layer on top of coffee
        if (progress > 0.05f) {
            val cremaTop = (coffeeTop - 2.dp.toPx()).coerceIn(bodyTop, bodyBottom)
            drawRect(
                color = coffeeFill.copy(alpha = 0.3f),
                topLeft = Offset(bodyLeft, cremaTop),
                size = Size(bodyWidth, 6.dp.toPx())
            )
        }
    }

    // Cup body outline
    drawPath(
        cupPath,
        color = cupColor,
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Rim
    drawRoundRect(
        color = cupColor,
        topLeft = Offset(bodyLeft - 4.dp.toPx(), bodyTop - h * 0.04f),
        size = Size(bodyWidth + 8.dp.toPx(), h * 0.08f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
    )

    // Handle
    drawArc(
        color = cupColor,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(bodyRight - handleRadius * 1.1f, handleCy - handleRadius),
        size = Size(handleRadius * 2.2f, handleRadius * 2),
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
    )

    // Steam (animated when exceeded)
    if (exceeded) {
        val steamColor = CoffeeFoam.copy(alpha = 0.6f)
        val steamW = 2.dp.toPx()
        val steamBase = bodyTop - h * 0.04f - 2.dp.toPx()
        for (i in 0 until 3) {
            val sx = w * (0.28f + i * 0.2f)
            val phase = (steamPhase + i * 0.33f) % 1f
            val riseOffset = phase * 24.dp.toPx()
            val alpha = 1f - phase * 0.6f

            drawArc(
                color = steamColor.copy(alpha = steamColor.alpha * alpha),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(sx - 4.dp.toPx(), steamBase - i * 6.dp.toPx() - riseOffset - 8.dp.toPx()),
                size = Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = steamW)
            )
            drawArc(
                color = steamColor.copy(alpha = steamColor.alpha * alpha),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(sx - 4.dp.toPx(), steamBase - i * 6.dp.toPx() - riseOffset - 16.dp.toPx()),
                size = Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = steamW)
            )
        }
    }
}
