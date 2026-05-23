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
    private val realTimeSyncService: RealTimeSyncService,
    private val sourceDeviceId: String = "watch"
) : CaffeineRepository {

    // --- Intake operations ---

    override suspend fun addIntake(intake: CaffeineIntake): Long {
        val stamped = intake.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        val id = intakeDao.insert(stamped)
        val synced = stamped.copy(id = id)
        propagateSync(
            entityType = "intake",
            entityId = id.toString(),
            operationType = PendingChangeEntity.OPERATION_INSERT,
            payload = SyncPayloadSerializer.serializeIntake(synced)
        )
        return id
    }

    override suspend fun updateIntake(intake: CaffeineIntake) {
        val stamped = intake.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        intakeDao.update(stamped)
        propagateSync(
            entityType = "intake",
            entityId = intake.id.toString(),
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            payload = SyncPayloadSerializer.serializeIntake(stamped)
        )
    }

    override suspend fun deleteIntake(intake: CaffeineIntake) {
        val stamped = intake.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        intakeDao.delete(stamped)
        propagateSync(
            entityType = "intake",
            entityId = intake.id.toString(),
            operationType = PendingChangeEntity.OPERATION_DELETE,
            payload = SyncPayloadSerializer.serializeIntake(stamped)
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
        val stamped = drink.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        val id = drinkDao.insert(stamped)
        propagateSync(
            entityType = "drink",
            entityId = id.toString(),
            operationType = PendingChangeEntity.OPERATION_INSERT,
            payload = SyncPayloadSerializer.serializeDrink(stamped.copy(id = id))
        )
        return id
    }

    override suspend fun updateDrink(drink: DrinkEntity) {
        val stamped = drink.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        drinkDao.update(stamped)
        propagateSync(
            entityType = "drink",
            entityId = drink.id.toString(),
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            payload = SyncPayloadSerializer.serializeDrink(stamped)
        )
    }

    override suspend fun deleteDrink(drink: DrinkEntity) {
        val stamped = drink.copy(
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceDeviceId = sourceDeviceId
        )
        drinkDao.delete(stamped)
        propagateSync(
            entityType = "drink",
            entityId = drink.id.toString(),
            operationType = PendingChangeEntity.OPERATION_DELETE,
            payload = SyncPayloadSerializer.serializeDrink(stamped)
        )
    }

    /**
     * Propagate a mutation to the paired phone via real-time sync.
     *
     * Fire-and-forget: errors are logged internally by [RealTimeSyncService]
     * and never propagated to the caller, so a sync failure never rolls
     * back the database operation.
     */
    @Suppress("TooGenericExceptionCaught")
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
        } catch (e: RuntimeException) {
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
