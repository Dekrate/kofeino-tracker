package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
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

    // --- Drink operations ---
    fun getAllDrinks(): Flow<List<DrinkEntity>>
    suspend fun getDrinkById(id: Long): DrinkEntity?
    suspend fun addDrink(drink: DrinkEntity): Long
    suspend fun updateDrink(drink: DrinkEntity)
    suspend fun deleteDrink(drink: DrinkEntity)
}
