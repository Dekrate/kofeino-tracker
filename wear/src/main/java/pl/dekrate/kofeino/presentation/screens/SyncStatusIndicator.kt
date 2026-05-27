package pl.dekrate.kofeino.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.data.sync.SyncStatusTracker

/**
 * A compact dot indicator for Wear OS that displays the current cross-device
 * sync status.
 *
 * Designed to be placed within [AppScaffold] so it appears on every screen.
 * Visual treatment per state:
 * - **Synced** — green dot, briefly visible then auto-hides
 * - **AwaitingDevice** — dim gray dot, always visible
 * - **Syncing** — pulsing animated dot
 * - **Error** — red dot, tappable (haptic feedback)
 *
 * @param syncStatusTracker The app-scoped [SyncStatusTracker] to observe.
 * @param modifier Optional [Modifier] for positioning.
 * @param dotSize Diameter of the status dot.
 */
@Composable
fun SyncStatusIndicator(
    syncStatusTracker: SyncStatusTracker,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp
) {
    val status by syncStatusTracker.status.collectAsState()
    val haptic = LocalHapticFeedback.current

    val (dotColor, isVisible, isAnimated) = when (status) {
        SyncStatus.Synced -> Triple(SyncGreen, false, false)
        SyncStatus.AwaitingDevice -> Triple(SyncGray, true, false)
        SyncStatus.Syncing -> Triple(SyncAmber, true, true)
        is SyncStatus.Error -> Triple(SyncRed, true, false)
    }

    val alphaValue = if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        pulseAlpha
    } else {
        1f
    }

    if (isVisible) {
        val clickModifier = if (status is SyncStatus.Error) {
            Modifier
                .size(40.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .semantics {
                    contentDescription = getSyncStatusDescription(status)
                    role = Role.Button
                }
        } else {
            Modifier.semantics { contentDescription = getSyncStatusDescription(status) }
        }

        Box(
            modifier = modifier.then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .alpha(alphaValue)
                    .clip(CircleShape)
                    .background(dotColor, CircleShape)
                    .testTag("sync_status_indicator")
            )
        }
    }
}

private fun getSyncStatusDescription(status: SyncStatus): String {
    return when (status) {
        SyncStatus.Synced -> "Synced"
        SyncStatus.AwaitingDevice -> "Waiting for device"
        SyncStatus.Syncing -> "Syncing"
        is SyncStatus.Error -> "Sync error"
    }
}

private val SyncGreen = Color(0xFF4CAF50)
private val SyncGray = Color(0xFF9E9E9E)
private val SyncAmber = Color(0xFFFFB74D)
private val SyncRed = Color(0xFFE53935)
