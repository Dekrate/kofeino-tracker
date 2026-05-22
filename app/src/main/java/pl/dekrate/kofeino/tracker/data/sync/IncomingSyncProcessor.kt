package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.wearable.MessageEvent
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes incoming sync messages from the paired Wear OS device.
 *
 * ## Flow
 * 1. Parse the [MessageEvent] path into entity type and operation type.
 * 2. Deserialize the payload JSON into a domain entity.
 * 3. Look up the current local state.
 * 4. Build [SyncChange] representations for both sides and resolve via
 *    [ConflictResolver] (Last-Write-Wins strategy).
 * 5. Apply the resolution directly to Room DAOs — **bypassing the repository
 *    layer** to prevent echo-loop re-sync.
 * 6. Log conflicts to [ConflictLogDao] for observability.
 *
 * ## Echo-loop prevention
 * This processor writes directly to DAOs rather than calling
 * [CaffeineRepositoryImpl]. The repository is the only caller of
 * [RealTimeSyncService], so inbound sync changes never trigger an
 * outbound re-broadcast.
 */
@Singleton
class IncomingSyncProcessor @Inject constructor(
    private val resolver: ConflictResolver,
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao,
    private val conflictLogDao: ConflictLogDao
) {
    companion object {
        private const val SYNC_PATH_PREFIX = "/sync"
    }

    /**
     * Result of processing a single incoming message.
     */
    enum class ProcessResult {
        /** Change was applied to the local database. */
        APPLIED,
        /** Change was skipped (local version kept or identical data). */
        SKIPPED,
        /** Message was not a sync message or was malformed. */
        IGNORED
    }

    /**
     * Process an incoming [MessageEvent] from the paired device.
     *
     * @param event The raw Wearable MessageEvent.
     * @return [ProcessResult] describing what action was taken.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun processIncoming(event: MessageEvent): ProcessResult {
        val path = event.path
        if (!path.startsWith(SYNC_PATH_PREFIX)) return ProcessResult.IGNORED

        val segments = path.removePrefix(SYNC_PATH_PREFIX)
            .split("/")
            .filter { it.isNotBlank() }
        if (segments.size != 2) {
            Timber.w("Malformed sync path: %s", path)
            return ProcessResult.IGNORED
        }

        val entityType = segments[0]
        val operationType = segments[1].uppercase(Locale.ROOT)
        val payload = event.data.toString(Charsets.UTF_8)

        Timber.d("Incoming sync: %s/%s (%dB)", entityType, operationType, payload.length)

        return try {
            when (entityType) {
                "intake" -> processIntake(operationType, payload)
                "drink" -> processDrink(operationType, payload)
                else -> {
                    Timber.w("Unknown entity type in sync message: %s", entityType)
                    ProcessResult.IGNORED
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            Timber.e(e, "Failed to process incoming sync: %s/%s", entityType, operationType)
            ProcessResult.IGNORED
        }
    }

    // ------------------------------------------------------------------
    // Intake processing
    // ------------------------------------------------------------------

    private suspend fun processIntake(
        operationType: String,
        payload: String
    ): ProcessResult {
        val incoming = try {
            SyncPayloadSerializer.deserializeIntake(payload)
        } catch (e: com.google.gson.JsonSyntaxException) {
            Timber.w(e, "Failed to deserialize intake payload")
            return ProcessResult.IGNORED
        }
        val local = intakeDao.getIntakeById(incoming.id)

        val localChange = local?.toSyncChange(
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            source = DeviceSource.PHONE
        )

        val incomingChange = incoming.toSyncChange(
            operationType = operationType,
            source = DeviceSource.WATCH
        )

        val resolution = resolver.resolve(localChange, incomingChange)
        return applyIntakeResolution(resolution, incoming, local)
    }

    private suspend fun applyIntakeResolution(
        resolution: ConflictResolution,
        incomingEntity: CaffeineIntake,
        localEntity: CaffeineIntake?
    ): ProcessResult {
        when (resolution) {
            is ConflictResolution.AcceptIncoming -> {
                when (resolution.change.operationType) {
                    PendingChangeEntity.OPERATION_DELETE -> {
                        if (localEntity != null) {
                            intakeDao.delete(localEntity)
                            Timber.d("Sync applied DELETE intake id=%s", localEntity.id)
                        }
                    }
                    else -> {
                        if (localEntity != null) {
                            intakeDao.update(incomingEntity)
                            Timber.d("Sync applied UPDATE intake id=%s", incomingEntity.id)
                        } else {
                            intakeDao.insert(incomingEntity)
                            Timber.d("Sync applied INSERT intake id=%s", incomingEntity.id)
                        }
                    }
                }
                logConflict(resolution)
                return ProcessResult.APPLIED
            }
            is ConflictResolution.KeepLocal -> {
                Timber.d("Sync kept local intake id=%s", resolution.change.entityId)
                logConflict(resolution)
                return ProcessResult.SKIPPED
            }
            is ConflictResolution.NoOp -> {
                Timber.d("Sync no-op intake id=%s", resolution.local.entityId)
                return ProcessResult.SKIPPED
            }
        }
    }

    // ------------------------------------------------------------------
    // Drink processing
    // ------------------------------------------------------------------

    private suspend fun processDrink(
        operationType: String,
        payload: String
    ): ProcessResult {
        val incoming = try {
            SyncPayloadSerializer.deserializeDrink(payload)
        } catch (e: com.google.gson.JsonSyntaxException) {
            Timber.w(e, "Failed to deserialize drink payload")
            return ProcessResult.IGNORED
        }
        val local = drinkDao.getDrinkById(incoming.id)

        val localChange = local?.toSyncChange(
            operationType = PendingChangeEntity.OPERATION_UPDATE,
            source = DeviceSource.PHONE
        )

        val incomingChange = incoming.toSyncChange(
            operationType = operationType,
            source = DeviceSource.WATCH
        )

        val resolution = resolver.resolve(localChange, incomingChange)
        return applyDrinkResolution(resolution, incoming, local)
    }

    private suspend fun applyDrinkResolution(
        resolution: ConflictResolution,
        incomingEntity: DrinkEntity,
        localEntity: DrinkEntity?
    ): ProcessResult {
        when (resolution) {
            is ConflictResolution.AcceptIncoming -> {
                when (resolution.change.operationType) {
                    PendingChangeEntity.OPERATION_DELETE -> {
                        if (localEntity != null) {
                            drinkDao.delete(localEntity)
                            Timber.d("Sync applied DELETE drink id=%s", localEntity.id)
                        }
                    }
                    else -> {
                        if (localEntity != null) {
                            drinkDao.update(incomingEntity)
                            Timber.d("Sync applied UPDATE drink id=%s", incomingEntity.id)
                        } else {
                            drinkDao.insert(incomingEntity)
                            Timber.d("Sync applied INSERT drink id=%s", incomingEntity.id)
                        }
                    }
                }
                logConflict(resolution)
                return ProcessResult.APPLIED
            }
            is ConflictResolution.KeepLocal -> {
                Timber.d("Sync kept local drink id=%s", resolution.change.entityId)
                logConflict(resolution)
                return ProcessResult.SKIPPED
            }
            is ConflictResolution.NoOp -> {
                Timber.d("Sync no-op drink id=%s", resolution.local.entityId)
                return ProcessResult.SKIPPED
            }
        }
    }

    // ------------------------------------------------------------------
    // Conflict logging
    // ------------------------------------------------------------------

    private suspend fun logConflict(resolution: ConflictResolution) {
        if (resolution is ConflictResolution.NoOp) return

        // By this point resolution is guaranteed AcceptIncoming or KeepLocal
        @Suppress("RedundantElseInWhen")
        val change = when (resolution) {
            is ConflictResolution.AcceptIncoming -> resolution.change
            is ConflictResolution.KeepLocal -> resolution.change
        }

        val localSource = DeviceSource.PHONE.name
        val incomingSource = change.source.name
        @Suppress("RedundantElseInWhen")
        val resolutionType = when (resolution) {
            is ConflictResolution.AcceptIncoming -> ConflictLogEntry.RESOLUTION_ACCEPT_INCOMING
            is ConflictResolution.KeepLocal -> ConflictLogEntry.RESOLUTION_KEEP_LOCAL
        }

        conflictLogDao.insert(
            ConflictLogEntry(
                entityType = change.entityType,
                entityId = change.entityId,
                localOperationType = PendingChangeEntity.OPERATION_UPDATE,
                incomingOperationType = change.operationType,
                localTimestamp = 0L, // not tracked at this level
                incomingTimestamp = change.timestamp,
                localSource = localSource,
                incomingSource = incomingSource,
                resolution = resolutionType
            )
        )
    }

    // ------------------------------------------------------------------
    // Extension functions for model → SyncChange conversion
    // ------------------------------------------------------------------

    private fun CaffeineIntake.toSyncChange(
        operationType: String,
        source: DeviceSource
    ): SyncChange = SyncChange(
        entityType = "intake",
        entityId = id.toString(),
        operationType = operationType,
        payload = SyncPayloadSerializer.serializeIntake(this),
        timestamp = timestamp,
        source = source
    )

    private fun DrinkEntity.toSyncChange(
        operationType: String,
        source: DeviceSource
    ): SyncChange = SyncChange(
        entityType = "drink",
        entityId = id.toString(),
        operationType = operationType,
        payload = SyncPayloadSerializer.serializeDrink(this),
        // NOTE: DrinkEntity has no timestamp field — wall-clock time is used for LWW resolution.
        // This means rapid conflicting drink edits on different devices may resolve based on
        // processing time rather than modification time.
        timestamp = System.currentTimeMillis(),
        source = source
    )
}
