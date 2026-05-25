package pl.dekrate.kofeino.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.dekrate.kofeino.common.sync.SyncStatus
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped tracker for cross-device sync status.
 *
 * Exposes a [StateFlow] that UI components can collect to display a glanceable
 * sync status indicator. The tracker receives state-change signals from:
 * - [WearableDataLayerManager] — node connection/disconnection
 * - [RealTimeSyncService] — send start / success / failure
 *
 * ## Design notes
 * - **Idempotent transitions**: setting the same status again is a no-op
 *   (no emission if the value has not changed).
 * - **Thread-safe**: `MutableStateFlow` guarantees thread-safe updates; all
 *   callers use coroutine contexts that are already correct.
 */
@Singleton
class SyncStatusTracker @Inject constructor() {

    private val _status = MutableStateFlow(SyncStatus.initial)
    
    /**
     * Observable sync status. UI composables collect this to render the
     * appropriate indicator.
     */
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Called when the set of reachable nodes with our sync capability changes.
     *
     * @param hasDevice `true` if at least one paired node is reachable.
     */
    fun onDeviceConnectionChanged(hasDevice: Boolean) {
        if (hasDevice) {
            _status.value = SyncStatus.Synced
        } else {
            _status.value = SyncStatus.AwaitingDevice
        }
        Timber.d("SyncStatus: deviceConnectionChanged(hasDevice=%s) → %s", hasDevice, _status.value)
    }

    /**
     * Called immediately before a sync payload is sent.
     */
    fun onSyncStarted() {
        _status.value = SyncStatus.Syncing
        Timber.d("SyncStatus: syncStarted → %s", _status.value)
    }

    /**
     * Called after a sync payload has been sent successfully.
     */
    fun onSyncCompleted() {
        _status.value = SyncStatus.Synced
        Timber.d("SyncStatus: syncCompleted → %s", _status.value)
    }

    /**
     * Called when a sync operation fails.
     *
     * @param error Human-readable error description.
     */
    fun onSyncFailed(error: String) {
        _status.value = SyncStatus.Error(error)
        Timber.w("SyncStatus: syncFailed(error=%s) → %s", error, _status.value)
    }
}
