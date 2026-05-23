@file:Suppress("TooGenericExceptionCaught")

package pl.dekrate.kofeino.data.sync

import androidx.room.withTransaction
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CancellationException
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes incoming sync messages from the paired phone on the watch module.
 *
 * ## Conflict Resolution (LWW)
 * Before applying any incoming change, this processor uses [ConflictResolver]
 * to detect and resolve conflicts using a Last-Write-Wins strategy:
 * - Compare lastModifiedTimestamp: the newer timestamp wins.
 * - Within 1ms tolerance: phone wins (phone is the primary device).
 * - Delete always wins over modification.
 * - Loser data is logged to the [ConflictLogDao] for user review.
 *
 * ## Flow
 * 1. Parse the [MessageEvent] path into entity type and operation type.
 * 2. Deserialize the payload JSON into a domain entity.
 * 3. Resolve conflict between local and incoming using [ConflictResolver].
 * 4. Apply the winning entity to Room DAOs — **bypassing the repository
 *    layer** to prevent echo-loop re-sync.
 *
 * ## Echo-loop prevention
 * This processor writes directly to DAOs rather than calling
 * [pl.dekrate.kofeino.data.repository.CaffeineRepositoryImpl].
 * The repository is the only caller of [RealTimeSyncService], so inbound
 * sync changes never trigger an outbound re-broadcast.
 */
@Singleton
class IncomingSyncProcessor @Inject constructor(
    private val database: CaffeineDatabase,
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
        /** Message was not a sync message or was malformed. */
        IGNORED
    }

    /**
     * Process an incoming [MessageEvent] from the paired phone.
     *
     * @param event The raw Wearable MessageEvent.
     * @return [ProcessResult] describing what action was taken.
     */
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to process incoming sync: %s/%s", entityType, operationType)
            ProcessResult.IGNORED
        }
    }

    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------

    private suspend fun applyIntakeChange(
        existing: CaffeineIntake?,
        result: ConflictResolver.ConflictResult<CaffeineIntake>,
        incoming: CaffeineIntake,
        operationType: String
    ): ProcessResult {
        val winner = result.winner
        when (operationType) {
            PendingChangeEntity.OPERATION_DELETE -> {
                if (existing != null) {
                    intakeDao.delete(existing)
                    Timber.d("Sync applied DELETE intake id=%s", incoming.id)
                } else {
                    Timber.d("Sync received DELETE for non-existent intake id=%s (ignored)", incoming.id)
                }
            }
            else -> {
                if (existing != null) {
                    intakeDao.update(winner)
                    Timber.d("Sync applied UPDATE intake id=%s", incoming.id)
                } else {
                    intakeDao.insert(winner)
                    Timber.d("Sync applied INSERT intake id=%s", incoming.id)
                }
            }
        }

        logConflictIfNeeded(result, existing, incoming, "intake")
        return ProcessResult.APPLIED
    }

    private suspend fun applyDrinkChange(
        existing: DrinkEntity?,
        result: ConflictResolver.ConflictResult<DrinkEntity>,
        incoming: DrinkEntity,
        operationType: String
    ): ProcessResult {
        val winner = result.winner
        when (operationType) {
            PendingChangeEntity.OPERATION_DELETE -> {
                if (existing != null) {
                    drinkDao.delete(existing)
                    Timber.d("Sync applied DELETE drink id=%s", incoming.id)
                } else {
                    Timber.d("Sync received DELETE for non-existent drink id=%s (ignored)", incoming.id)
                }
            }
            else -> {
                if (existing != null) {
                    drinkDao.update(winner)
                    Timber.d("Sync applied UPDATE drink id=%s", incoming.id)
                } else {
                    drinkDao.insert(winner)
                    Timber.d("Sync applied INSERT drink id=%s", incoming.id)
                }
            }
        }

        logConflictIfNeeded(result, existing, incoming, "drink")
        return ProcessResult.APPLIED
    }

    private suspend fun logConflictIfNeeded(
        result: ConflictResolver.ConflictResult<*>,
        existing: Any?,
        incoming: Any?,
        entityType: String
    ) {
        if (result.wasConflict) {
            conflictLogDao.log(
                ConflictLogEntity(
                    entityType = entityType,
                    entityId = when (incoming) {
                        is CaffeineIntake -> incoming.id.toString()
                        is DrinkEntity -> incoming.id.toString()
                        else -> ""
                    },
                    localEntityJson = ConflictResolver.serializeEntity(existing),
                    incomingEntityJson = ConflictResolver.serializeEntity(incoming),
                    decisionReason = result.reason,
                    winningSourceDeviceId = result.winner.let {
                        when (it) {
                            is CaffeineIntake -> it.sourceDeviceId
                            is DrinkEntity -> it.sourceDeviceId
                            else -> ""
                        }
                    }
                )
            )
            if (result.clockSkewWarning != null) {
                Timber.w(result.clockSkewWarning)
            }
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
        } catch (e: Exception) {
            Timber.w(e, "Failed to deserialize intake payload")
            return ProcessResult.IGNORED
        }

        return try {
            database.withTransaction {
                val existing = intakeDao.getIntakeById(incoming.id)
                val result = ConflictResolver.resolveIntakeConflict(
                    local = existing,
                    incoming = incoming,
                    operationType = operationType
                )
                applyIntakeChange(existing, result, incoming, operationType)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply incoming intake id=%s", incoming.id)
            ProcessResult.IGNORED
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
        } catch (e: Exception) {
            Timber.w(e, "Failed to deserialize drink payload")
            return ProcessResult.IGNORED
        }

        return try {
            database.withTransaction {
                val existing = drinkDao.getDrinkById(incoming.id)
                val result = ConflictResolver.resolveDrinkConflict(
                    local = existing,
                    incoming = incoming,
                    operationType = operationType
                )
                applyDrinkChange(existing, result, incoming, operationType)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply incoming drink id=%s", incoming.id)
            ProcessResult.IGNORED
        }
    }
}
