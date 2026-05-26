package pl.dekrate.kofeino.common.sync

/**
 * Snapshot of cross-device sync status information.
 *
 * Used by [CrossDeviceStatusScreen] on both phone and watch to display
 * a human-readable summary of the current synchronisation state.
 *
 * @property isPaired Whether a paired device is currently reachable.
 * @property pairedDeviceName Human-readable name of the paired device, or `null`.
 * @property lastEnqueuedTimestamp Epoch millis of the last enqueued change, or `null` if never synced.
 * @property pendingChangeCount Number of changes waiting to be sent.
 * @property failedChangeCount Number of changes that permanently failed.
 * @property conflictLogCount Number of conflict resolution entries recorded.
 * @property localAppVersion Version name of the local app.
 */
data class CrossDeviceStatus(
    val isPaired: Boolean,
    val pairedDeviceName: String?,
    val lastEnqueuedTimestamp: Long?,
    val pendingChangeCount: Int,
    val failedChangeCount: Int,
    val conflictLogCount: Int,
    val localAppVersion: String
) {
    companion object {
        /**
         * Default/initial state before any capability check completes.
         */
        val initial = CrossDeviceStatus(
            isPaired = false,
            pairedDeviceName = null,
            lastEnqueuedTimestamp = null,
            pendingChangeCount = 0,
            failedChangeCount = 0,
            conflictLogCount = 0,
            localAppVersion = ""
        )
    }
}
