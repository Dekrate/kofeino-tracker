package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository as CommonCaffeineRepository
import kotlinx.coroutines.flow.Flow

interface CaffeineRepository : CommonCaffeineRepository {

    // --- Phone-specific: get recent intakes (global history descending). ---
    fun getRecentIntakes(limit: Int = 100): Flow<List<CaffeineIntake>>

    // --- Phone-specific: search drinks by name. ---
    fun searchDrinks(query: String): Flow<List<DrinkEntity>>

    // --- Backup / Snapshot operations ---
    suspend fun getAllIntakesSnapshot(): List<CaffeineIntake>
    suspend fun getAllDrinksSnapshot(): List<DrinkEntity>
    suspend fun getAllIntakeIds(): List<Long>
    suspend fun getAllDrinkNames(): List<String>
    suspend fun bulkInsertIntakes(intakes: List<CaffeineIntake>)
    suspend fun bulkInsertDrinks(drinks: List<DrinkEntity>)
    suspend fun importAllAtomic(intakes: List<CaffeineIntake>, drinks: List<DrinkEntity>)
}
