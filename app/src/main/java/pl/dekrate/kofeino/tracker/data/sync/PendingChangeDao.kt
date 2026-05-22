package pl.dekrate.kofeino.tracker.data.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Data-access object for the [PendingChangeEntity] queue table.
 *
 * All operations are suspend functions so they can be called from
 * coroutine-backed services / view models.
 */
@Dao
interface PendingChangeDao {

    /** Insert a new pending change. Returns the generated row id. */
    @Insert
    suspend fun insert(change: PendingChangeEntity): Long

    /** Update an existing pending change (e.g. bump retryCount, change status). */
    @Update
    suspend fun update(change: PendingChangeEntity)

    /** Delete a specific pending change (e.g. after successful send). */
    @Delete
    suspend fun delete(change: PendingChangeEntity)

    /** Delete by primary key. */
    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Retrieve up to [limit] changes in PENDING status, oldest first (FIFO). */
    @Query("SELECT * FROM pending_changes WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingChanges(limit: Int = 100): List<PendingChangeEntity>

    /**
     * Retrieve any in-flight change for a specific entity (used for dedup).
     *
     * Matches both PENDING (awaiting send) and SENDING (in-flight) items
     * to prevent duplicate entries during a concurrent enqueue+flush.
     */
    @Query(
        """
        SELECT * FROM pending_changes
        WHERE entityType = :entityType AND entityId = :entityId
          AND status IN ('PENDING', 'SENDING')
        LIMIT 1
        """
    )
    suspend fun getPendingByEntity(entityType: String, entityId: String): PendingChangeEntity?

    /** Get total count of all rows in the queue. */
    @Query("SELECT COUNT(*) FROM pending_changes")
    suspend fun count(): Int

    /** Get count of FAILED changes. */
    @Query("SELECT COUNT(*) FROM pending_changes WHERE status = 'FAILED'")
    suspend fun countFailed(): Int

    /** Delete all changes (used for cleanup / testing). */
    @Query("DELETE FROM pending_changes")
    suspend fun deleteAll()

    /** Collect all permanently-FAILED rows (retry budget exhausted). */
    @Query("SELECT * FROM pending_changes WHERE status = 'FAILED'")
    suspend fun getFailedChanges(): List<PendingChangeEntity>

    /**
     * Collect FAILED items that still have retry budget left.
     * These were interrupted mid-flight or failed transiently
     * and can be re-attempted by [PendingSyncQueue.flushAllPending].
     */
    @Query("SELECT * FROM pending_changes WHERE status = 'FAILED' AND retryCount < 5")
    suspend fun getRetryableFailed(): List<PendingChangeEntity>
}
