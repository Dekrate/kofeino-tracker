package com.example.kofeinotracker.data.repository

import com.example.kofeinotracker.domain.model.CaffeineIntake
import kotlinx.coroutines.flow.Flow

interface CaffeineRepository {
    suspend fun addIntake(intake: CaffeineIntake)
    fun getTodayIntakes(): Flow<List<CaffeineIntake>>
    fun getTodayTotalCaffeine(): Flow<Int>
    suspend fun clearAll()
}
