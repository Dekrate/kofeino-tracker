package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import kotlinx.coroutines.flow.Flow

interface CaffeineRepository {

    // --- Intake operations ---
    suspend fun addIntake(intake: CaffeineIntake): Long
    suspend fun updateIntake(intake: CaffeineIntake)
    suspend fun deleteIntake(intake: CaffeineIntake)
    suspend fun getIntakeById(id: Long): CaffeineIntake?
    fun getIntakesForDate(dateMillis: Long): Flow<List<CaffeineIntake>>
    fun getTotalCaffeineForDate(dateMillis: Long): Flow<Int>
    suspend fun clearAll()

    /** Phone-specific: get recent intakes (global history descending). */
    fun getRecentIntakes(limit: Int = 100): Flow<List<CaffeineIntake>>

    // --- Drink operations ---
    fun getAllDrinks(): Flow<List<DrinkEntity>>
    suspend fun getDrinkById(id: Long): DrinkEntity?
    suspend fun addDrink(drink: DrinkEntity): Long
    suspend fun updateDrink(drink: DrinkEntity)
    suspend fun deleteDrink(drink: DrinkEntity)

    /** Phone-specific: search drinks by name. */
    fun searchDrinks(query: String): Flow<List<DrinkEntity>>
}
