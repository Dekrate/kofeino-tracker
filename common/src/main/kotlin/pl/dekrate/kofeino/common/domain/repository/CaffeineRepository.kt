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

    // --- Search & Recent ---
    fun searchDrinks(query: String): Flow<List<DrinkEntity>>
    fun getRecentIntakes(limit: Int = 5): Flow<List<CaffeineIntake>>

    // --- Backup / Snapshot operations (phone-specific, default throws) ---
    /**
     * Returns all intakes as a non-observable snapshot.
     * Used by backup/restore operations on phone.
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun getAllIntakesSnapshot(): List<CaffeineIntake> =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Returns all drinks as a non-observable snapshot.
     * Used by backup/restore operations on phone.
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun getAllDrinksSnapshot(): List<DrinkEntity> =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Returns all intake IDs for conflict resolution during restore.
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun getAllIntakeIds(): List<Long> =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Returns all drink names for conflict resolution during restore.
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun getAllDrinkNames(): List<String> =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Bulk-insert intakes atomically (part of backup restore).
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun bulkInsertIntakes(intakes: List<CaffeineIntake>): Unit =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Bulk-insert drinks atomically (part of backup restore).
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun bulkInsertDrinks(drinks: List<DrinkEntity>): Unit =
        throw UnsupportedOperationException("Backup operations not supported on this device")

    /**
     * Atomically import intakes and drinks in a single transaction (backup restore).
     * @throws UnsupportedOperationException on devices that don't support backup.
     */
    suspend fun importAllAtomic(intakes: List<CaffeineIntake>, drinks: List<DrinkEntity>): Unit =
        throw UnsupportedOperationException("Backup operations not supported on this device")
}
