package pl.dekrate.kofeino.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConflictLogDao {

    @Insert
    suspend fun log(entry: ConflictLogEntity)

    @Query("SELECT * FROM conflict_log ORDER BY resolvedAt DESC")
    suspend fun getAll(): List<ConflictLogEntity>

    @Query("SELECT COUNT(*) FROM conflict_log")
    suspend fun count(): Int

    @Query("DELETE FROM conflict_log")
    suspend fun clearAll()

    /** Emits the total number of conflict log entries on every change. */
    @Query("SELECT COUNT(*) FROM conflict_log")
    fun observeCount(): Flow<Int>
}
