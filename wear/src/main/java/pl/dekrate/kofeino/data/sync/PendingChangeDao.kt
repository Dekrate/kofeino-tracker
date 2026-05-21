package pl.dekrate.kofeino.data.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Data-access object for the [PendingChangeEntity] queue table (wear module).
 *
 * ⚠ Mirror of the phone module DAO — keep in sync.
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

    /** Retrieve all changes in PENDING status, oldest first (FIFO). */
    @Query("SELECT * FROM pending_changes WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingChanges(): List<PendingChangeEntity>

    /** Retrieve a single pending change for a specific entity (used for dedup). */
    @Query(
        """
        SELECT * FROM pending_changes
        WHERE entityType = :entityType AND entityId = :entityId AND status = 'PENDING'
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

    /** Collect all FAILED rows whose retry budget is exhausted. */
    @Query("SELECT * FROM pending_changes WHERE status = 'FAILED'")
    suspend fun getFailedChanges(): List<PendingChangeEntity>
}
