package pl.dekrate.kofeino.tracker.presentation.viewmodel

// ⚠ Mirror of wear/src/main/.../CrossDeviceStatusViewModel.kt — keep in sync

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.CapabilityClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.sync.CrossDeviceStatus
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogDao
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeDao
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.data.sync.WearableDataLayerManager
import pl.dekrate.kofeino.tracker.data.sync.await
import timber.log.Timber
import javax.inject.Inject

data class CrossDeviceStatusUiState(
    val syncStatus: SyncStatus = SyncStatus.initial,
    val deviceStatus: CrossDeviceStatus = CrossDeviceStatus.initial
)

@HiltViewModel
class CrossDeviceStatusViewModel @Inject constructor(
    private val syncStatusTracker: SyncStatusTracker,
    private val pendingChangeDao: PendingChangeDao,
    private val conflictLogDao: ConflictLogDao,
    private val capabilityClient: CapabilityClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrossDeviceStatusUiState())
    val uiState: StateFlow<CrossDeviceStatusUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStatus()
    }

    private fun loadDeviceStatus() {
        viewModelScope.launch {
            val versionName = resolveVersionName()
            // Resolve static device info ONCE before starting live combine
            val initialPairedDeviceName = resolvePairedDeviceName()
            val initialLastEnqueuedTimestamp = resolveLastEnqueuedTimestamp()

            // Emit initial state immediately with resolved values
            _uiState.value = CrossDeviceStatusUiState(
                syncStatus = SyncStatus.initial,
                deviceStatus = CrossDeviceStatus(
                    isPaired = initialPairedDeviceName != null,
                    pairedDeviceName = initialPairedDeviceName,
                    lastEnqueuedTimestamp = initialLastEnqueuedTimestamp,
                    pendingChangeCount = 0,
                    failedChangeCount = 0,
                    conflictLogCount = 0,
                    localAppVersion = versionName
                )
            )

            // Then start live updates via combine (device name stays static)
            combine(
                syncStatusTracker.status,
                pendingChangeDao.observeCount(),
                pendingChangeDao.observeFailedCount(),
                conflictLogDao.observeCount()
            ) { syncStatus, pendingCount, failedCount, conflictCount ->
                val lastEnqueuedTimestamp = resolveLastEnqueuedTimestamp()

                CrossDeviceStatusUiState(
                    syncStatus = syncStatus,
                    deviceStatus = CrossDeviceStatus(
                        isPaired = initialPairedDeviceName != null,
                        pairedDeviceName = initialPairedDeviceName,
                        lastEnqueuedTimestamp = lastEnqueuedTimestamp,
                        pendingChangeCount = pendingCount,
                        failedChangeCount = failedCount,
                        conflictLogCount = conflictCount,
                        localAppVersion = versionName
                    )
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun resolveVersionName(): String {
        return try {
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            info.versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Failed to resolve app version name")
            ""
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun resolvePairedDeviceName(): String? {
        return try {
            val task = capabilityClient
                .getCapability(
                    WearableDataLayerManager.SYNC_CAPABILITY_NAME,
                    CapabilityClient.FILTER_REACHABLE
                )
            val capabilityInfo = if (task.isComplete) {
                task.result
            } else {
                task.await()
            }
            capabilityInfo.nodes.firstOrNull()?.displayName
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Timber.w(e, "Failed to resolve paired device name")
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun resolveLastEnqueuedTimestamp(): Long? {
        return try {
            pendingChangeDao.getLatestEnqueuedTimestamp()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Timber.w(e, "Failed to resolve last sync timestamp")
            null
        }
    }
}
