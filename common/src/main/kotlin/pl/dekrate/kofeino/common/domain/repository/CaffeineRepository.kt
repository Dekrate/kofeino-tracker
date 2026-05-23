package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity

/**
 * Shared repository contract for caffeine intake and drink operations.
 *
 * This interface mirrors the method signatures from the per-module
 * repository interfaces but operates on the canonical [CaffeineIntake]
 * and [DrinkEntity] domain models defined in the `:common` module.
 *
 * Implementations are responsible for mapping between these shared
 * domain models and the module-specific Room entities.
 */
interface CaffeineRepository {

    // --- Intake operations ---
    suspend fun addIntake(intake: CaffeineIntake): Long
    suspend fun updateIntake(intake: CaffeineIntake)
    suspend fun deleteIntake(intake: CaffeineIntake)
    suspend fun getIntakeById(id: Long): CaffeineIntake?
    fun getIntakesForDate(date: LocalDate): Flow<List<CaffeineIntake>>
    fun getTotalCaffeineForDate(date: LocalDate): Flow<Int>
    suspend fun clearAll()

    // --- Drink operations ---
    fun getAllDrinks(): Flow<List<DrinkEntity>>
    suspend fun getDrinkById(id: Long): DrinkEntity?
    suspend fun addDrink(drink: DrinkEntity): Long
    suspend fun updateDrink(drink: DrinkEntity)
    suspend fun deleteDrink(drink: DrinkEntity)
}
