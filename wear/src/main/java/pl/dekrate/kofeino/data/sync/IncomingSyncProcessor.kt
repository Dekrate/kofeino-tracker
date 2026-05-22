@file:Suppress("TooGenericExceptionCaught")

package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.MessageEvent
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
 * ## Design
 * Unlike the app module's [pl.dekrate.kofeino.tracker.data.sync.IncomingSyncProcessor],
 * this processor has **no ConflictResolver** — the watch is a secondary device and
 * always accepts changes from the phone as authoritative.
 *
 * ## Flow
 * 1. Parse the [MessageEvent] path into entity type and operation type.
 * 2. Deserialize the payload JSON into a domain entity.
 * 3. Apply the change directly to Room DAOs — **bypassing the repository
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
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            Timber.w(e, "Failed to deserialize intake payload")
            return ProcessResult.IGNORED
        }

        return try {
            when (operationType) {
                PendingChangeEntity.OPERATION_DELETE -> {
                    intakeDao.delete(incoming)
                    Timber.d("Sync applied DELETE intake id=%s", incoming.id)
                }
                else -> {
                    val existing = intakeDao.getIntakeById(incoming.id)
                    if (existing != null) {
                        intakeDao.update(incoming)
                        Timber.d("Sync applied UPDATE intake id=%s", incoming.id)
                    } else {
                        intakeDao.insert(incoming)
                        Timber.d("Sync applied INSERT intake id=%s", incoming.id)
                    }
                }
            }
            ProcessResult.APPLIED
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
            when (operationType) {
                PendingChangeEntity.OPERATION_DELETE -> {
                    drinkDao.delete(incoming)
                    Timber.d("Sync applied DELETE drink id=%s", incoming.id)
                }
                else -> {
                    val existing = drinkDao.getDrinkById(incoming.id)
                    if (existing != null) {
                        drinkDao.update(incoming)
                        Timber.d("Sync applied UPDATE drink id=%s", incoming.id)
                    } else {
                        drinkDao.insert(incoming)
                        Timber.d("Sync applied INSERT drink id=%s", incoming.id)
                    }
                }
            }
            ProcessResult.APPLIED
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply incoming drink id=%s", incoming.id)
            ProcessResult.IGNORED
        }
    }
}
