package pl.dekrate.kofeino.common.sync

import java.util.UUID

/**
 * Represents the state of a full-sync session between paired devices.
 *
 * Used by [FullSyncManager] on both phone and watch to track
 * the lifecycle of a synchronisation session and prevent concurrent
 * full-sync operations.
 *
 * ## State machine
 * ```
 * Idle → Requesting → AwaitingResponse → Applying → Idle
 *                        ↕ (bidirectional)
 *                  Responding → SendingData → Idle
 * ```
 */
sealed interface SyncSessionState {

    /**
     * No sync session is in progress.
     * This is the initial and resting state.
     */
    data object Idle : SyncSessionState

    /**
     * This device initiated a full-sync request and is waiting
     * for the paired device's [SyncEvent.FullSyncResponse].
     *
     * @property nodeId The target node ID.
     * @property sessionId Unique identifier for this session.
     * @property startTimestamp When the request was sent.
     */
    data class Requesting(
        val nodeId: String,
        val sessionId: String,
        val startTimestamp: Long
    ) : SyncSessionState

    /**
     * This device received a [SyncEvent.FullSyncRequest] from the
     * paired device and is preparing a [SyncEvent.FullSyncResponse].
     *
     * @property requestNodeId The source node ID of the request.
     * @property sessionId Unique identifier for this session.
     * @property stateHash The state hash from the request.
     * @property lastSyncTimestamp The last sync timestamp from the request.
     */
    data class Responding(
        val requestNodeId: String,
        val sessionId: String,
        val stateHash: String,
        val lastSyncTimestamp: Long
    ) : SyncSessionState

    /**
     * This device is waiting for the paired device to send its data
     * after this device sent its own data (bidirectional phase).
     *
     * @property nodeId The target node ID.
     * @property sessionId Unique identifier for this session.
     */
    data class AwaitingResponse(
        val nodeId: String,
        val sessionId: String
    ) : SyncSessionState

    companion object {
        /** Generate a unique session identifier. */
        fun generateSessionId(): String = UUID.randomUUID().toString()

        /**
         * Validates whether a transition from [current] to [next] is allowed.
         * @return `true` if the transition is valid, `false` otherwise.
         */
        fun canTransitionTo(current: SyncSessionState, next: SyncSessionState): Boolean = when (current) {
            is Idle -> next is Requesting || next is Responding
            is Requesting -> next is AwaitingResponse
            is AwaitingResponse -> next is Idle
            is Responding -> next is Idle
        }
    }
}
