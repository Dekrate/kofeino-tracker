package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import timber.log.Timber

/**
 * A compact status chip for the phone app's TopAppBar that displays the
 * current cross-device sync status.
 *
 * Visual treatment per state:
 * - **Synced** — green [Icons.Filled.Done], auto-hides after 3 seconds
 * - **AwaitingDevice** — gray [Icons.Filled.Close], always visible
 * - **Syncing** — animated [CircularProgressIndicator], always visible
 * - **Error** — red [Icons.Filled.Warning], tappable (logs the error)
 *
 * @param syncStatusTracker The app-scoped [SyncStatusTracker] to observe.
 * @param modifier Optional [Modifier] for positioning within the TopAppBar.
 */
@Composable
fun SyncStatusChip(
    syncStatusTracker: SyncStatusTracker,
    modifier: Modifier = Modifier
) {
    val state by syncStatusTracker.status.collectAsState()
    val context = LocalContext.current

    // Auto-hide Synced after 3s — timer resets on each new Synced emission
    var isVisible by remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        if (state is SyncStatus.Synced) {
            delay(SyncedAutoHideDelayMs)
            isVisible = false
        } else {
            isVisible = true
        }
    }

    val config: Config = when (val status = state) {
        SyncStatus.Synced -> Config(
            icon = Icons.Filled.Done,
            contentDescription = context.getString(R.string.sync_status_synced),
            tint = Color(0xFF4CAF50),
            showProgress = false
        )
        SyncStatus.AwaitingDevice -> Config(
            icon = Icons.Filled.Close,
            contentDescription = context.getString(R.string.sync_status_awaiting_device),
            tint = Color(0xFF9E9E9E),
            showProgress = false
        )
        SyncStatus.Syncing -> Config(
            icon = null,
            contentDescription = context.getString(R.string.sync_status_syncing),
            tint = MaterialTheme.colorScheme.primary,
            showProgress = true
        )
        is SyncStatus.Error -> Config(
            icon = Icons.Filled.Warning,
            contentDescription = context.getString(R.string.sync_status_error, status.message),
            tint = Color(0xFFE53935),
            showProgress = false,
            isError = true,
            errorMessage = status.message
        )
    }

    SyncStatusContent(
        config = config,
        isVisible = isVisible,
        modifier = modifier
    )
}

@Composable
private fun SyncStatusContent(
    config: Config,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .size(24.dp)
                .then(
                    if (config.isError) {
                        Modifier.clickable {
                            Timber.w("Sync error: %s", config.errorMessage)
                        }
                    } else {
                        Modifier
                    }
                )
                .semantics { this.contentDescription = config.contentDescription },
            contentAlignment = Alignment.Center
        ) {
            if (config.showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = config.tint
                )
            } else if (config.icon != null) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = config.contentDescription,
                    tint = config.tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Configuration bundle for the sync status visual.
 *
 * Using a private data class rather than destructuring a tuple keeps the
 * mapping from [SyncStatus] → visual properties explicit and type-safe.
 */
private data class Config(
    val icon: ImageVector?,
    val contentDescription: String,
    val tint: Color,
    val showProgress: Boolean,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

private const val SyncedAutoHideDelayMs = 3_000L
