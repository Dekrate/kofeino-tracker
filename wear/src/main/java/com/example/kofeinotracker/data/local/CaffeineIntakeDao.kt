package com.example.kofeinotracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.kofeinotracker.domain.model.CaffeineIntake
import kotlinx.coroutines.flow.Flow

@Dao
interface CaffeineIntakeDao {

    @Insert
    suspend fun insert(intake: CaffeineIntake): Long

    @Query("SELECT * FROM caffeine_intakes WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getTodayIntakes(startOfDay: Long, endOfDay: Long): Flow<List<CaffeineIntake>>

    @Query("DELETE FROM caffeine_intakes")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(caffeineMg), 0) FROM caffeine_intakes WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getTodayTotalCaffeine(startOfDay: Long, endOfDay: Long): Flow<Int>
}
