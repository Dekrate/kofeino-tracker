package pl.dekrate.kofeino.tracker.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data-access object for the [ConflictLogEntry] table.
 *
 * Provides insert, query-by-entity, and full-sweep queries for
 * debugging / auditing conflict resolutions.
 */
@Dao
interface ConflictLogDao {

    /** Persist a conflict resolution entry. Returns the generated row id. */
    @Insert
    suspend fun insert(entry: ConflictLogEntry): Long

    /** Retrieve all log entries for a specific entity, most recent first. */
    @Query(
        """
        SELECT * FROM conflict_log
        WHERE entityType = :entityType AND entityId = :entityId
        ORDER BY resolvedAt DESC
        """
    )
    suspend fun getByEntity(entityType: String, entityId: String): List<ConflictLogEntry>

    /** Retrieve all log entries, most recent first. */
    @Query("SELECT * FROM conflict_log ORDER BY resolvedAt DESC")
    suspend fun getAll(): List<ConflictLogEntry>

    /** Count total entries in the conflict log. */
    @Query("SELECT COUNT(*) FROM conflict_log")
    suspend fun count(): Int

    /** Delete all entries (used for testing / cleanup). */
    @Query("DELETE FROM conflict_log")
    suspend fun deleteAll()

    /** Retrieve all entries that recorded a clock-skew warning. */
    @Query("SELECT * FROM conflict_log WHERE clockSkewMs > 0 ORDER BY resolvedAt DESC")
    suspend fun getClockSkewEntries(): List<ConflictLogEntry>
}
