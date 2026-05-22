package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.sync.PendingChangeEntity
import pl.dekrate.kofeino.data.sync.RealTimeSyncService
import pl.dekrate.kofeino.data.sync.SyncPayloadSerializer
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao,
    private val realTimeSyncService: RealTimeSyncService
) : CaffeineRepository {

    // --- Intake operations ---

    override suspend fun addIntake(intake: CaffeineIntake): Long {
        val id = intakeDao.insert(intake)
        val synced = intake.copy(id = id)
        propagateSync(
            entityType = "intake",
            entityId = id.toString(),
            operationType = PendingChangeEntity.OPERATION_INSERT,
            payload = SyncPayloadSerializer.serializeIntake(synced)
        )
        return id
    }

    override suspend fun updateIntake(intake: CaffeineIntake) {
        intakeDao.update(intake)
        propagateSync(
            entityType = "intake",
            entityId = intake.id.toString(),
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            payload = SyncPayloadSerializer.serializeIntake(intake)
        )
    }

    override suspend fun deleteIntake(intake: CaffeineIntake) {
        intakeDao.delete(intake)
        propagateSync(
            entityType = "intake",
            entityId = intake.id.toString(),
            operationType = PendingChangeEntity.OPERATION_DELETE,
            payload = SyncPayloadSerializer.serializeIntake(intake)
        )
    }

    override fun getIntakesForDate(dateMillis: Long): Flow<List<CaffeineIntake>> {
        val (start, end) = dayBounds(dateMillis)
        return intakeDao.getIntakesByDate(start, end)
    }

    override fun getTotalCaffeineForDate(dateMillis: Long): Flow<Int> {
        val (start, end) = dayBounds(dateMillis)
        return intakeDao.getTotalCaffeineByDate(start, end)
    }

    override suspend fun getIntakeById(id: Long): CaffeineIntake? {
        return intakeDao.getIntakeById(id)
    }

    override suspend fun clearAll() {
        intakeDao.deleteAll()
        // No sync propagation for mass clear — it's a local-only operation
    }

    // --- Drink operations ---

    override fun getAllDrinks(): Flow<List<DrinkEntity>> {
        return drinkDao.getAllDrinks()
    }

    override suspend fun getDrinkById(id: Long): DrinkEntity? {
        return drinkDao.getDrinkById(id)
    }

    override suspend fun addDrink(drink: DrinkEntity): Long {
        val id = drinkDao.insert(drink)
        propagateSync(
            entityType = "drink",
            entityId = id.toString(),
            operationType = PendingChangeEntity.OPERATION_INSERT,
            payload = SyncPayloadSerializer.serializeDrink(drink.copy(id = id))
        )
        return id
    }

    override suspend fun updateDrink(drink: DrinkEntity) {
        drinkDao.update(drink)
        propagateSync(
            entityType = "drink",
            entityId = drink.id.toString(),
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            payload = SyncPayloadSerializer.serializeDrink(drink)
        )
    }

    override suspend fun deleteDrink(drink: DrinkEntity) {
        drinkDao.delete(drink)
        propagateSync(
            entityType = "drink",
            entityId = drink.id.toString(),
            operationType = PendingChangeEntity.OPERATION_DELETE,
            payload = SyncPayloadSerializer.serializeDrink(drink)
        )
    }

    /**
     * Propagate a mutation to the paired phone via real-time sync.
     *
     * Fire-and-forget: errors are logged internally by [RealTimeSyncService]
     * and never propagated to the caller, so a sync failure never rolls
     * back the database operation.
     */
    private suspend fun propagateSync(
        entityType: String,
        entityId: String,
        operationType: String,
        payload: String
    ) {
        try {
            realTimeSyncService.propagateChange(entityType, entityId, operationType, payload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Sync propagation failed for %s/%s id=%s", entityType, operationType, entityId)
        }
    }

    /**
     * Zwraca parę (początekDnia, koniecDnia) dla podanego timestampu.
     *
     * Używa Calendar.add(DAY_OF_YEAR, 1) zamiast +86400000,
     * aby poprawnie obsłużyć zmiany czasu (DST).
     * W dni "cofnięcia" (fall back, 25h) obejmuje wszystkie 25h,
     * w dni "przeskoku" (spring forward, 23h) nie wychodzi poza dobę.
     */
    private fun dayBounds(millis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return start to end
    }
}
