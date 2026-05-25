package pl.dekrate.kofeino.common.sync

/**
 * Represents the current state of cross-device data synchronisation.
 *
 * Used by both the phone (`,:app`) and Wear OS (`,:wear`) modules to display a
 * glanceable sync status indicator in the UI.
 *
 * Using a **sealed interface** guarantees exhaustive `when` matching at compile
 * time, eliminating the possibility of an unhandled status.
 */
sealed interface SyncStatus {

    /**
     * All local data is synchronised with the paired device. The indicator
     * should auto-hide after a short delay in this state.
     */
    data object Synced : SyncStatus

    /**
     * No paired device is currently reachable via the Wearable Data Layer.
     * The indicator remains visible to remind the user that changes are not
     * being propagated.
     */
    data object AwaitingDevice : SyncStatus

    /**
     * A synchronisation operation is in progress (sending or receiving).
     * The indicator should show an animated / busy visual.
     */
    data object Syncing : SyncStatus

    /**
     * A synchronisation operation failed. The indicator should show an error
     * visual that the user can tap for details.
     *
     * @property message Human-readable error description.
     */
    data class Error(val message: String) : SyncStatus

    companion object {
        /** The initial status assumed before any capability check completes. */
        val initial: SyncStatus = AwaitingDevice
    }
}
