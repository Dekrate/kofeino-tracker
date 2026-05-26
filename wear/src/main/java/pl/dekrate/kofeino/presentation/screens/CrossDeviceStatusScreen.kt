package pl.dekrate.kofeino.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.presentation.viewmodel.CrossDeviceStatusUiState
import pl.dekrate.kofeino.presentation.viewmodel.CrossDeviceStatusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compact Cross-Device Status screen for Wear OS.
 *
 * Shows:
 * - Paired device info chip + sync status dot
 * - Sync queue depth (pending / failed)
 * - Conflict log count
 * - Last sync timestamp
 * - Local app version
 */
@Composable
fun CrossDeviceStatusScreen(
    modifier: Modifier = Modifier,
    viewModel: CrossDeviceStatusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listScrollState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listScrollState) { contentPadding ->
        TransformingLazyColumn(
            state = listScrollState,
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
        ) {
            // ── Header ──
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.cross_device_status_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ── Device Info ──
            item {
                DeviceInfoCard(
                    status = state.deviceStatus,
                    syncStatus = state.syncStatus
                )
            }

            item { SyncQueueContent(state) }
            item { ConflictLogContent(state) }
            item { LastSyncContent(state) }
            item { AppVersionContent(state) }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    status: pl.dekrate.kofeino.common.sync.CrossDeviceStatus,
    syncStatus: SyncStatus
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (status.isPaired) {
                status.pairedDeviceName ?: stringResource(R.string.paired_device)
            } else {
                stringResource(R.string.no_paired_device)
            },
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SyncStatusDot(syncStatus = syncStatus)
    }
}

@Composable
private fun SyncStatusDot(syncStatus: SyncStatus) {
    val color = when (syncStatus) {
        SyncStatus.Synced -> MaterialTheme.colorScheme.primary
        SyncStatus.AwaitingDevice -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.Syncing -> MaterialTheme.colorScheme.tertiary
        is SyncStatus.Error -> MaterialTheme.colorScheme.error
    }
    val label = when (syncStatus) {
        SyncStatus.Synced -> stringResource(R.string.sync_status_synced)
        SyncStatus.AwaitingDevice -> stringResource(R.string.sync_status_awaiting_device)
        SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
        is SyncStatus.Error -> syncStatus.message
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SyncQueueContent(state: CrossDeviceStatusUiState) {
    Column {
        ListHeader {
            Text(
                text = stringResource(R.string.sync_queue_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        StatRow(
            label = stringResource(R.string.pending_changes),
            value = state.deviceStatus.pendingChangeCount.toString()
        )
        StatRow(
            label = stringResource(R.string.failed_changes),
            value = state.deviceStatus.failedChangeCount.toString()
        )
    }
}

@Composable
private fun ConflictLogContent(state: CrossDeviceStatusUiState) {
    Column {
        ListHeader {
            Text(
                text = stringResource(R.string.conflict_log_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        StatRow(
            label = stringResource(R.string.conflict_entries),
            value = state.deviceStatus.conflictLogCount.toString()
        )
    }
}

@Composable
private fun LastSyncContent(state: CrossDeviceStatusUiState) {
    Column {
        ListHeader {
            Text(
                text = stringResource(R.string.last_sync_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        val lastSyncText = state.deviceStatus.lastEnqueuedTimestamp?.let { ts ->
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(ts))
        } ?: stringResource(R.string.never_synced)
        StatRow(
            label = stringResource(R.string.last_sync_time),
            value = lastSyncText
        )
    }
}

@Composable
private fun AppVersionContent(state: CrossDeviceStatusUiState) {
    Column {
        ListHeader {
            Text(
                text = stringResource(R.string.about),
                style = MaterialTheme.typography.titleMedium
            )
        }
        StatRow(
            label = stringResource(R.string.app_version),
            value = state.deviceStatus.localAppVersion
        )
    }
}
