package pl.dekrate.kofeino.tracker.data.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity that records the history of a single conflict resolution.
 *
 * Each row captures the local and incoming state at the time of resolution,
 * the decision that was made, and the clock-skew window (if any).
 *
 * This log is useful for:
 * - Debugging unexpected sync behaviour.
 * - Auditing data-loss scenarios (e.g. accidental overwrite).
 * - Detecting chronic clock-skew between paired devices.
 */
@Entity(
    tableName = "conflict_log",
    indices = [
        Index(value = ["entityType", "entityId"], name = "idx_conflict_entity"),
        Index(value = ["resolvedAt"], name = "idx_conflict_resolved_at")
    ]
)
data class ConflictLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Entity type involved in the conflict — e.g. "intake", "drink". */
    val entityType: String,

    /** Entity identifier involved in the conflict. */
    val entityId: String,

    /** The current local operation type when the conflict was resolved. */
    val localOperationType: String,

    /** The incoming operation type when the conflict was resolved. */
    val incomingOperationType: String,

    /** Timestamp of the local change (epoch millis). */
    val localTimestamp: Long,

    /** Timestamp of the incoming change (epoch millis). */
    val incomingTimestamp: Long,

    /** Source of the local device — "PHONE" or "WATCH". */
    val localSource: String,

    /** Source of the incoming change — "PHONE" or "WATCH". */
    val incomingSource: String,

    /** The resolution that was applied: "KEEP_LOCAL", "ACCEPT_INCOMING", or "NO_OP". */
    val resolution: String,

    /** Millisecond skew detected (0 if within tolerance). */
    val clockSkewMs: Long = 0,

    /** Millis timestamp of when this resolution was recorded. */
    val resolvedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val RESOLUTION_KEEP_LOCAL = "KEEP_LOCAL"
        const val RESOLUTION_ACCEPT_INCOMING = "ACCEPT_INCOMING"
        const val RESOLUTION_NO_OP = "NO_OP"
    }
}
