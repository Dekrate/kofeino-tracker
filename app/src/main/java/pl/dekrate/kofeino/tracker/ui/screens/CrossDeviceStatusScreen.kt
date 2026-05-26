package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.presentation.viewmodel.CrossDeviceStatusUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.CrossDeviceStatusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full Cross-Device Status screen for the phone app.
 *
 * Shows:
 * - Paired device card with sync status indicator
 * - Sync queue depth (pending / failed)
 * - Conflict log summary
 * - Last sync timestamp
 * - Local app version
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossDeviceStatusScreen(
    syncStatusTracker: SyncStatusTracker,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrossDeviceStatusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val backDesc = stringResource(R.string.back)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.cross_device_status_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = backDesc }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDesc,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    SyncStatusChip(syncStatusTracker = syncStatusTracker)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Device Info Card ──
            SectionHeader(stringResource(R.string.paired_device), Modifier.padding(bottom = 8.dp))
            DeviceInfoCard(state.deviceStatus)

            SyncStatusSection(state)

            // ── Last Sync ──
            SectionHeader(stringResource(R.string.last_sync_title), Modifier.padding(bottom = 8.dp))
            val lastSyncText = state.deviceStatus.lastEnqueuedTimestamp?.let { ts ->
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.format(Date(ts))
            } ?: stringResource(R.string.never_synced)
            StatRow(stringResource(R.string.last_sync_time), lastSyncText)

            SectionDivider()

            // ── App Version ──
            SectionHeader(stringResource(R.string.about), Modifier.padding(bottom = 8.dp))
            StatRow(stringResource(R.string.app_version), state.deviceStatus.localAppVersion)
        }
    }
}

@Composable
private fun SyncStatusSection(state: CrossDeviceStatusUiState) {
    Column {
        SectionDivider()

        // ── Sync Status ──
        SectionHeader(stringResource(R.string.sync_status_title), Modifier.padding(bottom = 8.dp))
        val syncStatusLabel = when (val status = state.syncStatus) {
            SyncStatus.Synced -> stringResource(R.string.sync_status_synced)
            SyncStatus.AwaitingDevice -> stringResource(R.string.sync_status_awaiting_device)
            SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
            is SyncStatus.Error -> stringResource(R.string.sync_status_error, status.message)
        }
        StatRow(stringResource(R.string.status), syncStatusLabel)

        SectionDivider()

        // ── Sync Queue ──
        SectionHeader(stringResource(R.string.sync_queue_title), Modifier.padding(bottom = 8.dp))
        StatRow(stringResource(R.string.pending_changes), state.deviceStatus.pendingChangeCount.toString())
        StatRow(stringResource(R.string.failed_changes), state.deviceStatus.failedChangeCount.toString())

        SectionDivider()

        // ── Conflict Log ──
        SectionHeader(stringResource(R.string.conflict_log_title), Modifier.padding(bottom = 8.dp))
        StatRow(stringResource(R.string.conflict_entries), state.deviceStatus.conflictLogCount.toString())
    }
}

@Composable
private fun DeviceInfoCard(
    status: pl.dekrate.kofeino.common.sync.CrossDeviceStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (status.isPaired) {
                    status.pairedDeviceName ?: stringResource(R.string.paired_device)
                } else {
                    stringResource(R.string.no_paired_device)
                },
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (status.isPaired) {
                    stringResource(R.string.device_connected)
                } else {
                    stringResource(R.string.device_disconnected)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (status.isPaired) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
