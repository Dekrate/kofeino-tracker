package pl.dekrate.kofeino.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
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
    safeLimit: Int = 400,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "coffeeProgress"
    )

    val cupColor = if (exceeded) CoffeeColors.errorRed else CoffeeColors.latte
    val coffeeFill = when {
        exceeded -> CoffeeColors.errorRed.copy(alpha = 0.5f)
        animatedProgress > 0.75f -> CoffeeColors.mediumRoast
        animatedProgress > 0.4f -> CoffeeColors.lightRoast
        else -> CoffeeColors.darkRoast
    }

    val safeLimitText = stringResource(R.string.safe_limit_format, safeLimit)
    val contentDesc = if (exceeded) {
        "${stringResource(R.string.limit_exceeded)}: $total mg"
    } else {
        "$total mg / $safeLimitText"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCoffeeCup(
                    progress = animatedProgress,
                    exceeded = exceeded,
                    cupColor = cupColor,
                    coffeeFill = coffeeFill
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
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (exceeded) {
            Text(
                text = stringResource(R.string.limit_exceeded),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            Text(
                text = stringResource(R.string.safe_limit_format, safeLimit),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun DrawScope.drawCoffeeCup(
    progress: Float,
    exceeded: Boolean,
    cupColor: Color,
    coffeeFill: Color
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
            val offset = i * 6.dp.toPx()
            drawArc(
                color = steamColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(sx - 4.dp.toPx(), steamBase - offset - 8.dp.toPx()),
                size = Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = steamW)
            )
            drawArc(
                color = steamColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(sx - 4.dp.toPx(), steamBase - offset - 16.dp.toPx()),
                size = Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = steamW)
            )
        }
    }
}
