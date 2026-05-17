package pl.dekrate.kofeino.tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import kotlinx.coroutines.flow.Flow

@Dao
interface CaffeineIntakeDao {

    @Insert
    suspend fun insert(intake: CaffeineIntake): Long

    @Update
    suspend fun update(intake: CaffeineIntake)

    @Delete
    suspend fun delete(intake: CaffeineIntake)

    @Query("SELECT * FROM caffeine_intakes WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getIntakesByDate(startOfDay: Long, endOfDay: Long): Flow<List<CaffeineIntake>>

    @Query("SELECT * FROM caffeine_intakes WHERE id = :id")
    suspend fun getIntakeById(id: Long): CaffeineIntake?

    @Query("DELETE FROM caffeine_intakes")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(caffeineMg), 0) FROM caffeine_intakes WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getTotalCaffeineByDate(startOfDay: Long, endOfDay: Long): Flow<Int>

    /** Phone-specific: get all intakes ordered by timestamp (for history screen). */
    @Query("SELECT * FROM caffeine_intakes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentIntakes(limit: Int = 100): Flow<List<CaffeineIntake>>

    /** Phone-specific: delete intakes older than a given timestamp. */
    @Query("DELETE FROM caffeine_intakes WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
