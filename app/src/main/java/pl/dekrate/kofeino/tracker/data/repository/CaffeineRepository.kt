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

    // --- Backup / Snapshot operations ---

    /** Snapshot all intakes (non-observable, for backup export). */
    suspend fun getAllIntakesSnapshot(): List<CaffeineIntake>

    /** Snapshot all drinks (non-observable, for backup export). */
    suspend fun getAllDrinksSnapshot(): List<DrinkEntity>

    /** Snapshot all intake IDs (for backup import conflict resolution). */
    suspend fun getAllIntakeIds(): List<Long>

    /** Snapshot all drink names (for backup import conflict resolution). */
    suspend fun getAllDrinkNames(): List<String>

    /** Bulk insert intakes (for backup import). */
    suspend fun bulkInsertIntakes(intakes: List<CaffeineIntake>)

    /** Bulk insert drinks (for backup import). */
    suspend fun bulkInsertDrinks(drinks: List<DrinkEntity>)

    /**
     * Atomic import: inserts intakes and drinks in a single Room transaction.
     * If either insert fails, the entire import is rolled back.
     */
    suspend fun importAllAtomic(intakes: List<CaffeineIntake>, drinks: List<DrinkEntity>)
}
