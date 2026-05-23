package pl.dekrate.kofeino.common.sync

/**
 * Sealed type hierarchy representing every event the sync protocol can
 * produce or consume.
 *
 * Using a sealed interface guarantees exhaustive `when` matching at
 * compile time, eliminating the possibility of unhandled sync events.
 *
 * @property timestamp       Epoch millis of when the event was created.
 * @property sourceDeviceId  Stable identifier of the device that created the event.
 */
sealed interface SyncEvent {
    val timestamp: Long
    val sourceDeviceId: String

    /** A new caffeine intake was added on the source device. */
    data class IntakeAdded(
        val intakeJson: String,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** An existing caffeine intake was deleted on the source device. */
    data class IntakeDeleted(
        val intakeId: Long,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** An existing caffeine intake was modified on the source device. */
    data class IntakeUpdated(
        val intakeJson: String,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** A new drink definition was created on the source device. */
    data class DrinkCreated(
        val drinkJson: String,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** An existing drink definition was modified on the source device. */
    data class DrinkUpdated(
        val drinkJson: String,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** An existing drink definition was deleted on the source device. */
    data class DrinkDeleted(
        val drinkId: Long,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** Settings were changed on the source device. */
    data class SettingsChanged(
        val settingsJson: String,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** Request a full sync — sent when a device first connects. */
    data class FullSyncRequest(
        val stateHash: String,
        val lastSyncTimestamp: Long,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** Response containing a batch of entities for full sync. */
    data class FullSyncResponse(
        val entityType: SyncEntityType,
        val entitiesJson: List<String>,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent

    /** Acknowledge receipt of a sync batch. */
    data class SyncAck(
        val stateHash: String,
        val receivedEntityTypes: List<SyncEntityType>,
        override val timestamp: Long,
        override val sourceDeviceId: String,
    ) : SyncEvent
}
