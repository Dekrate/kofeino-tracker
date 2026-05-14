package pl.dekrate.kofeino.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pl.dekrate.kofeino.domain.model.CaffeineIntake
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

    @Query("DELETE FROM caffeine_intakes")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(caffeineMg), 0) FROM caffeine_intakes WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getTotalCaffeineByDate(startOfDay: Long, endOfDay: Long): Flow<Int>
}
