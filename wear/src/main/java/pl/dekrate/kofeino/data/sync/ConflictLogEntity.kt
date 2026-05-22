package pl.dekrate.kofeino.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that records the outcome of a conflict resolution between
 * local and incoming (synced-from-phone) data.
 *
 * Loser data is never deleted — it is persisted here so the user can
 * review what happened during a sync conflict.
 */
@Entity(tableName = "conflict_log")
data class ConflictLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val localEntityJson: String,
    val incomingEntityJson: String,
    val decisionReason: String,
    val resolvedAt: Long = System.currentTimeMillis(),
    val winningSourceDeviceId: String = ""
)
