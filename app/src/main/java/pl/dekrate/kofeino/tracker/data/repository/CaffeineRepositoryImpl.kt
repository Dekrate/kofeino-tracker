package pl.dekrate.kofeino.tracker.data.repository

import androidx.room.withTransaction
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity
import pl.dekrate.kofeino.tracker.data.sync.RealTimeSyncService
import pl.dekrate.kofeino.tracker.data.sync.SyncPayloadSerializer
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake as CommonCaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity as CommonDrinkEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao,
    private val database: CaffeineDatabase,
    private val realTimeSyncService: RealTimeSyncService,
    private val sourceDeviceId: String
) : CaffeineRepository {

    // --- Intake operations (from CommonCaffeineRepository) ---

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

    override suspend fun getIntakeById(id: Long): CommonCaffeineIntake? {
        return intakeDao.getIntakeById(id)?.toCommon()
    }

    override fun getIntakesForDate(date: LocalDate): Flow<List<CommonCaffeineIntake>> {
        val (start, end) = dayBounds(date.toEpochMillis())
        return intakeDao.getIntakesByDate(start, end).map { list ->
            list.map { it.toCommon() }
        }
    }

    override fun getTotalCaffeineForDate(date: LocalDate): Flow<Int> {
        val (start, end) = dayBounds(date.toEpochMillis())
        return intakeDao.getTotalCaffeineByDate(start, end)
    }

    override suspend fun clearAll() {
        intakeDao.deleteAll()
    }

    // --- App-specific: Recent intakes ---

    override fun getRecentIntakes(limit: Int): Flow<List<CommonCaffeineIntake>> {
        return intakeDao.getRecentIntakes(limit).map { list ->
            list.map { it.toCommon() }
        }
    }

    // --- Drink operations (from CommonCaffeineRepository) ---

    override fun getAllDrinks(): Flow<List<CommonDrinkEntity>> {
        return drinkDao.getAllDrinks().map { list ->
            list.map { it.toCommon() }
        }
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
        val synced = stamped.copy(id = id)
        propagateSync(
            entityType = "drink",
            entityId = id.toString(),
            operationType = PendingChangeEntity.OPERATION_INSERT,
            payload = SyncPayloadSerializer.serializeDrink(synced)
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

    // --- App-specific: Search drinks ---

    override fun searchDrinks(query: String): Flow<List<CommonDrinkEntity>> {
        return drinkDao.searchDrinks(query).map { list ->
            list.map { it.toCommon() }
        }
    }

    // --- Backup / Snapshot operations ---

    override suspend fun getAllIntakesSnapshot(): List<CommonCaffeineIntake> =
        intakeDao.getAllIntakesSnapshot().map { it.toCommon() }

    override suspend fun getAllDrinksSnapshot(): List<CommonDrinkEntity> =
        drinkDao.getAllDrinksSnapshot().map { it.toCommon() }

    override suspend fun getAllIntakeIds(): List<Long> =
        intakeDao.getAllIntakeIds()

    override suspend fun getAllDrinkNames(): List<String> =
        drinkDao.getAllDrinkNames()

    override suspend fun bulkInsertIntakes(intakes: List<CommonCaffeineIntake>) {
        intakeDao.insertAll(intakes.map { it.toEntity() })
    }

    override suspend fun bulkInsertDrinks(drinks: List<CommonDrinkEntity>) {
        drinkDao.insertAll(drinks.map { it.toEntity() })
    }

    override suspend fun importAllAtomic(intakes: List<CommonCaffeineIntake>, drinks: List<CommonDrinkEntity>) {
        database.withTransaction {
            intakeDao.insertAll(intakes.map { it.toEntity() })
            drinkDao.insertAll(drinks.map { it.toEntity() })
        }
    }

    // --- Sync propagation ---

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

    // --- DST-safe day bounds ---

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

/**
 * Converts a [kotlinx.datetime.LocalDate] to epoch millis at start of day
 * in the system-default time zone.
 */
internal fun LocalDate.toEpochMillis(): Long {
    return this.atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}
