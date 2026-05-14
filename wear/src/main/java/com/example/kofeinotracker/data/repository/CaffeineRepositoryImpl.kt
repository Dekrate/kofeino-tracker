package com.example.kofeinotracker.data.repository

import com.example.kofeinotracker.data.local.CaffeineIntakeDao
import com.example.kofeinotracker.domain.model.CaffeineIntake
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val dao: CaffeineIntakeDao
) : CaffeineRepository {

    override suspend fun addIntake(intake: CaffeineIntake) {
        dao.insert(intake)
    }

    override fun getTodayIntakes(): Flow<List<CaffeineIntake>> {
        val (start, end) = dayBounds()
        return dao.getTodayIntakes(start, end)
    }

    override fun getTodayTotalCaffeine(): Flow<Int> {
        val (start, end) = dayBounds()
        return dao.getTodayTotalCaffeine(start, end)
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    private fun dayBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = start + 86_400_000L
        return start to end
    }
}
