package pl.dekrate.kofeino.tracker.data.sync

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-device sync state tracking for mid-sync disconnect rollback.
 *
 * ## State machine per device
 *
 * ```
 *       ┌──────────┐
 *       │   IDLE    │ ◄────────────┐
 *       └────┬──────┘              │
 *            │ onSyncStarted()      │
 *            ▼                     │
 *       ┌──────────┐              │
 *       │  SYNCING │──────────────┤
 *       └────┬──────┘              │
 *            │ onSyncCompleted()    │
 *            ├─────────────────────┘
 *            │ onSyncFailed() / onDeviceDisconnected()
 *            ▼
 *       ┌──────────┐
 *       │  FAILED  │──────────────► (needsFullResync=true → on next connect triggers full resync)
 *       └──────────┘
 * ```
 *
 * ## Thread safety
 * Uses [MutableStateFlow.update] for atomic read-modify-write of the device state map.
 * All public methods are safe to call from any coroutine dispatcher.
 *
 * ## Crash recovery
 * If the service crashes mid-sync, Room's [androidx.room.withTransaction] in
 * [IncomingSyncProcessor] ensures batch atomicity. The [SyncStateStore] is
 * not updated → next [FullSyncManager] cycle detects state hash divergence →
 * triggers full resync. No explicit rollback needed here.
 */
@Singleton
class SyncDeviceStateManager @Inject constructor() {

    private val _deviceStates = MutableStateFlow<Map<String, SyncDeviceState>>(emptyMap())

    /**
     * Observable map of device sync states, keyed by Wearable node ID.
     * UI components can collect this to display per-device sync status.
     */
    val deviceStates: StateFlow<Map<String, SyncDeviceState>> = _deviceStates.asStateFlow()

    /**
     * Called when a sync operation starts for the given device.
     * If already SYNCING, this is a no-op (idempotent).
     */
    fun onSyncStarted(deviceId: String) {
        _deviceStates.update { current ->
            val existing = current[deviceId] ?: SyncDeviceState(deviceId = deviceId)
            if (existing.state == SyncDeviceState.State.SYNCING) {
                Timber.d("SyncDeviceState: already SYNCING for %s, skipping", deviceId)
                return@update current
            }
            current + (deviceId to existing.copy(
                state = SyncDeviceState.State.SYNCING,
                lastError = null
            ))
        }
    }

    /**
     * Called when a sync operation completes successfully.
     * Resets [needsFullResync] so the next connection doesn't unnecessarily
     * retrigger a full sync.
     */
    fun onSyncCompleted(deviceId: String) {
        val now = System.currentTimeMillis()
        _deviceStates.update { current ->
            val existing = current[deviceId] ?: SyncDeviceState(deviceId = deviceId)
            current + (deviceId to existing.copy(
                state = SyncDeviceState.State.IDLE,
                lastError = null,
                lastSyncTimestamp = now,
                needsFullResync = false
            ))
        }
        Timber.d("SyncDeviceState: %s → IDLE (completed)", deviceId)
    }

    /**
     * Called when a sync operation fails.
     * If the failure interrupted a SYNCING session, [needsFullResync] is set
     * to ensure the next connection triggers a full data reconciliation.
     */
    fun onSyncFailed(deviceId: String, error: String) {
        _deviceStates.update { current ->
            val existing = current[deviceId] ?: SyncDeviceState(deviceId = deviceId)
            val wasSyncing = existing.state == SyncDeviceState.State.SYNCING
            current + (deviceId to existing.copy(
                state = SyncDeviceState.State.FAILED,
                lastError = error,
                needsFullResync = wasSyncing || existing.needsFullResync
            ))
        }
        Timber.d("SyncDeviceState: %s → FAILED (error=%s)", deviceId, error)
    }

    /**
     * Called when a device disconnects while mid-sync.
     *
     * Room's [androidx.room.withTransaction] in [IncomingSyncProcessor] already
     * ensures atomic batch processing — partial writes are rolled back at the
     * DB level. This method marks the device for full resync on reconnect.
     *
     * @param deviceId The node ID that disconnected.
     */
    fun onDeviceDisconnected(deviceId: String) {
        _deviceStates.update { current ->
            val existing = current[deviceId] ?: return@update current
            if (existing.state == SyncDeviceState.State.SYNCING) {
                current + (deviceId to existing.copy(
                    state = SyncDeviceState.State.FAILED,
                    lastError = "Device disconnected mid-sync",
                    needsFullResync = true
                ))
            } else {
                current // Already IDLE or FAILED — no transition needed
            }
        }
    }

    /**
     * Called when a device connects.
     *
     * @return `true` if the device had [needsFullResync] set, indicating
     *         the caller ([FullSyncManager]) should trigger a full sync.
     */
    fun onDeviceConnected(deviceId: String): Boolean {
        val oldState = _deviceStates.getAndUpdate { current ->
            val existing = current[deviceId]
            current + (deviceId to SyncDeviceState(
                deviceId = deviceId,
                lastSyncTimestamp = existing?.lastSyncTimestamp ?: 0L,
                needsFullResync = false // Reset — FullSyncManager will handle it
            ))
        }
        val needsResync = oldState[deviceId]?.needsFullResync ?: false

        Timber.d("SyncDeviceState: %s connected (needsResync=%s)", deviceId, needsResync)
        return needsResync
    }

    /** Returns the current state for a device, or IDLE if unknown. */
    fun getState(deviceId: String): SyncDeviceState {
        return _deviceStates.value[deviceId] ?: SyncDeviceState(deviceId = deviceId)
    }

    /** Returns true if any device is currently in SYNCING state. */
    fun hasActiveSync(): Boolean {
        return _deviceStates.value.any { it.value.state == SyncDeviceState.State.SYNCING }
    }

    /** Clear all state. Used in testing. */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun reset() {
        _deviceStates.value = emptyMap()
    }
}

/**
 * Immutable snapshot of per-device sync state.
 */
data class SyncDeviceState(
    val deviceId: String,
    val state: SyncDeviceState.State = State.IDLE,
    val lastError: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val needsFullResync: Boolean = false
) {
    enum class State {
        /** No active sync — device may or may not be connected. */
        IDLE,

        /** Actively syncing data with this device. */
        SYNCING,

        /** Last sync attempt failed. May need full resync on reconnect. */
        FAILED
    }
}
