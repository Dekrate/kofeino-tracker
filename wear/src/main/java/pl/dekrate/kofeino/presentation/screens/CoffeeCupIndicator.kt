package pl.dekrate.kofeino.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.presentation.theme.CoffeeColors

/**
 * A coffee cup indicator that fills up based on caffeine progress.
 * Shows a drawn coffee cup on Canvas, steam when limit exceeded,
 * and caffeine amount text overlaid.
 */
@Composable
fun CoffeeCupIndicator(
    total: Int,
    progress: Float,
    exceeded: Boolean,
    modifier: Modifier = Modifier,
    safeLimit: Int = 400
) {
    val safeProgress = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 600),
        label = "coffeeProgress"
    )

    val cupColor = computeCupColor(exceeded)
    val coffeeFill = computeCoffeeFill(exceeded, animatedProgress)
    val steamPhase = computeSteamPhase(exceeded)
    val ringColor = computeRingColor(exceeded, animatedProgress, cupColor)
    val trackColor = CoffeeColors.cream.copy(alpha = 0.2f)

    val safeLimitText = stringResource(R.string.safe_limit_format, safeLimit)
    val limitExceededText = stringResource(R.string.limit_exceeded)

    val contentDesc = if (exceeded) {
        "$limitExceededText: $total mg"
    } else {
        "$total mg / $safeLimitText"
    }

    val counterDesc = if (exceeded) {
        "$limitExceededText: $total mg"
    } else {
        "$total mg"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier
                .size(120.dp)
                .semantics { hideFromAccessibility() }
            ) {
                drawProgressRing(
                    progress = animatedProgress,
                    ringColor = ringColor,
                    trackColor = trackColor,
                    strokeWidth = 4.dp.toPx()
                )
                drawCoffeeCup(
                    progress = animatedProgress,
                    exceeded = exceeded,
                    cupColor = cupColor,
                    coffeeFill = coffeeFill,
                    steamPhase = steamPhase
                )
            }
            // Dark pill background behind the caffeine counter for contrast
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    modifier = Modifier.semantics {
                        contentDescription = counterDesc
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        CoffeeCupLabel(
            exceeded = exceeded,
            total = total,
            safeLimit = safeLimit,
            safeLimitText = safeLimitText
        )
    }
}

private fun computeCupColor(exceeded: Boolean): Color =
    if (exceeded) CoffeeColors.errorRed else CoffeeColors.latte

@Suppress("MagicNumber")
private fun computeCoffeeFill(exceeded: Boolean, animatedProgress: Float): Color = when {
    exceeded -> CoffeeColors.errorRed.copy(alpha = 0.5f)
    animatedProgress > 0.75f -> CoffeeColors.mediumRoast
    animatedProgress > 0.4f -> CoffeeColors.lightRoast
    else -> CoffeeColors.darkRoast
}

@Suppress("MagicNumber")
private fun computeRingColor(
    exceeded: Boolean,
    animatedProgress: Float,
    cupColor: Color
): Color = when {
    exceeded -> CoffeeColors.errorRed
    animatedProgress >= 1f -> cupColor
    animatedProgress > 0.6f -> CoffeeColors.cream
    else -> CoffeeColors.cream.copy(alpha = 0.5f)
}

@Composable
private fun computeSteamPhase(exceeded: Boolean): Float {
    if (!exceeded) return 0f

    val transition = rememberInfiniteTransition(label = "steam")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamPhase"
    )
    return phase
}

@Composable
private fun CoffeeCupLabel(
    exceeded: Boolean,
    total: Int,
    safeLimit: Int,
    safeLimitText: String
) {
    if (exceeded) {
        val exceededDesc = stringResource(R.string.limit_exceeded)
        Text(
            text = exceededDesc,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.semantics {
                contentDescription = "$exceededDesc: $total mg / $safeLimitText"
            }
        )
    } else {
        val todayDesc = stringResource(R.string.today_caffeine)
        Text(
            text = stringResource(R.string.safe_limit_format, safeLimit),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.semantics {
                contentDescription = "$todayDesc: $total mg / $safeLimitText"
            }
        )
    }
}

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
        sweepAngle = 360f * (progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f),
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
    val strokeW = 3.dp.toPx()

    // --- Cup body dimensions ---
    val bodyLeft = w * 0.2f
    val bodyRight = w * 0.72f
    val bodyTop = h * 0.22f
    val bodyBottom = h * 0.82f
    val bodyWidth = bodyRight - bodyLeft
    val bodyHeight = bodyBottom - bodyTop

    // --- Coffee fill region ---
    val fillableHeight = bodyHeight * 0.88f
    val coffeeTop = bodyBottom - fillableHeight * progress

    // --- Handle ---
    val handleCy = bodyTop + bodyHeight * 0.42f
    val handleRadius = bodyWidth * 0.2f

    // --- Build cup body path (rounded rect, slightly tapered) ---
    val cupPath = Path().apply {
        val taper = bodyWidth * 0.1f
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

    // --- Coffee fill (clipped to cup body) ---
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

    // --- Cup body outline ---
    drawPath(
        cupPath,
        color = cupColor,
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // --- Rim ---
    drawRoundRect(
        color = cupColor,
        topLeft = Offset(w * 0.14f, bodyTop - h * 0.04f),
        size = Size(w * 0.64f, h * 0.08f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
    )

    // --- Handle ---
    drawArc(
        color = cupColor,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(bodyRight - handleRadius * 1.1f, handleCy - handleRadius),
        size = Size(handleRadius * 2.2f, handleRadius * 2),
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
    )

    // --- Steam (when exceeded) ---
    if (exceeded) {
        val steamColor = CoffeeColors.foam.copy(alpha = 0.6f)
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
