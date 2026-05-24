package pl.dekrate.kofeino.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake as CommonCaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity as CommonDrinkEntity
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.sync.PendingChangeEntity
import pl.dekrate.kofeino.data.sync.RealTimeSyncService
import pl.dekrate.kofeino.data.sync.SyncPayloadSerializer
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class CaffeineRepositoryImpl @Inject constructor(
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao,
    private val realTimeSyncService: RealTimeSyncService,
) : CaffeineRepository {

    private val sourceDeviceId = "watch"

    // --- Intake operations ---

    override suspend fun addIntake(intake: CommonCaffeineIntake): Long {
        val entity = intake.toEntity()
        val stamped = entity.copy(
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

    override suspend fun updateIntake(intake: CommonCaffeineIntake) {
        val entity = intake.toEntity()
        val stamped = entity.copy(
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

    override suspend fun deleteIntake(intake: CommonCaffeineIntake) {
        val entity = intake.toEntity()
        val stamped = entity.copy(
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

    override fun getIntakesForDate(date: LocalDate): Flow<List<CommonCaffeineIntake>> {
        val millis = date.toEpochMillis()
        val (start, end) = dayBounds(millis)
        return intakeDao.getIntakesByDate(start, end).map { list ->
            list.map { it.toCommon() }
        }
    }

    override fun getTotalCaffeineForDate(date: LocalDate): Flow<Int> {
        val millis = date.toEpochMillis()
        val (start, end) = dayBounds(millis)
        return intakeDao.getTotalCaffeineByDate(start, end)
    }

    override suspend fun getIntakeById(id: Long): CommonCaffeineIntake? {
        return intakeDao.getIntakeById(id)?.toCommon()
    }

    override suspend fun clearAll() {
        intakeDao.deleteAll()
        // No sync propagation for mass clear — it's a local-only operation
    }

    // --- Drink operations ---

    override fun getAllDrinks(): Flow<List<CommonDrinkEntity>> {
        return drinkDao.getAllDrinks().map { list -> list.map { it.toCommon() } }
    }

    override suspend fun getDrinkById(id: Long): CommonDrinkEntity? {
        return drinkDao.getDrinkById(id)?.toCommon()
    }

    override suspend fun addDrink(drink: CommonDrinkEntity): Long {
        val entity = drink.toEntity()
        val stamped = entity.copy(
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

    override suspend fun updateDrink(drink: CommonDrinkEntity) {
        val entity = drink.toEntity()
        val stamped = entity.copy(
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

    override suspend fun deleteDrink(drink: CommonDrinkEntity) {
        val entity = drink.toEntity()
        val stamped = entity.copy(
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

    // --- Search & Recent ---

    override fun searchDrinks(query: String): Flow<List<CommonDrinkEntity>> {
        return drinkDao.searchDrinks(query).map { list ->
            list.map { it.toCommon() }
        }
    }

    override fun getRecentIntakes(limit: Int): Flow<List<CommonCaffeineIntake>> {
        return intakeDao.getRecentIntakes(limit).map { list ->
            list.map { it.toCommon() }
        }
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
     * Returns (dayStart, dayEnd) for the given timestamp.
     *
     * Uses Calendar.add(DAY_OF_YEAR, 1) instead of +86400000
     * to correctly handle DST changes.
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

    private fun LocalDate.toEpochMillis(): Long {
        return this.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}
