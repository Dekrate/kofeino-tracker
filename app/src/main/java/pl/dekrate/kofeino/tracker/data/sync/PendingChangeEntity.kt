package pl.dekrate.kofeino.tracker.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single pending data change queued for cross-device sync.
 *
 * Each row describes an operation (INSERT/UPDATE/DELETE) on a domain entity
 * (e.g. "intake" or "drink") that must be transmitted to the paired device.
 *
 * The queue is persisted so changes survive an app restart (offline-first).
 */
@Entity(tableName = "pending_changes")
data class PendingChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Logical entity type — e.g. "intake", "drink". */
    val entityType: String,

    /** Stable identifier for the entity (as a string — Room PK may be Long). */
    val entityId: String,

    /** Operation type: "INSERT", "UPDATE", or "DELETE". */
    val operationType: String,

    /** JSON-serialised payload of the entity at the time of change. */
    val payload: String,

    /** Millis timestamp of when the change was enqueued (used for ordering). */
    val timestamp: Long,

    /** Number of failed send attempts so far. */
    val retryCount: Int = 0,

    /** Current status: PENDING, SENDING, FAILED. */
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENDING = "SENDING"
        const val STATUS_FAILED = "FAILED"

        const val OPERATION_INSERT = "INSERT"
        const val OPERATION_UPDATE = "UPDATE"
        const val OPERATION_DELETE = "DELETE"

        const val MAX_RETRIES = 5
    }
}
