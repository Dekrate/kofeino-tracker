package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed, offline-first queue for pending cross-device sync changes (wear module).
 *
 * ⚠ Mirror of the phone module — keep in sync.
 *
 * @param nodeId target node to send messages to.
 *               In production this is obtained via [CapabilityClient].
 */
@Singleton
class PendingSyncQueue @Inject constructor(
    private val dao: PendingChangeDao,
    private val messageClient: MessageClient,
    private val nodeId: String
) {
    companion object {
        const val SYNC_PATH_PREFIX = "/sync"
        const val SYNC_PATH_FORMAT = "$SYNC_PATH_PREFIX/%s/%s"
        private const val MAX_RETRIES = 5
        private const val FLUSH_BATCH_SIZE = 100
        private const val BACKOFF_BASE_MS = 1000L
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Enqueue a change for later sync.
     *
     * **Dedup**: if a PENDING change already exists for the same
     * (entityType, entityId), it is replaced with the latest operation
     * and payload.
     */
    suspend fun enqueue(
        entityType: String,
        entityId: String,
        operationType: String,
        payload: String
    ) {
        val existing = dao.getPendingByEntity(entityType, entityId)
        if (existing != null) {
            Timber.d("Dedup – replacing pending %s/%s (%s → %s)",
                entityType, entityId, existing.operationType, operationType)
            dao.update(
                existing.copy(
                    operationType = operationType,
                    payload = payload,
                    timestamp = System.currentTimeMillis(),
                    retryCount = 0,
                    status = PendingChangeEntity.STATUS_PENDING
                )
            )
        } else {
            dao.insert(
                PendingChangeEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operationType = operationType,
                    payload = payload,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Flush up to [FLUSH_BATCH_SIZE] PENDING changes to the paired device.
     *
     * Each change is sent via [MessageClient.sendMessage] on a typed path:
     *   `/sync/<entityType>/<operationType>`
     *
     * On success → change is removed from the queue.
     * On failure → retry count is bumped, **exponential backoff** is applied
     *              (1s, 2s, 4s, 8s, 16s), and if it reaches [MAX_RETRIES] the
     *              change is marked FAILED.
     *
     * @return [FlushResult] summarising sent / failed counts.
     */
    suspend fun flush(): FlushResult {
        val pending = dao.getPendingChanges(limit = FLUSH_BATCH_SIZE)
        if (pending.isEmpty()) return FlushResult(0, 0)

        var sent = 0
        var failed = 0

        for (change in pending) {
            if (change.retryCount > 0) {
                val backoffMs = backoffDelay(change.retryCount)
                Timber.d("Backoff %dms for %s/%s (retry #%d)",
                    backoffMs, change.entityType, change.entityId, change.retryCount)
                delay(backoffMs)
            }

            dao.update(change.copy(status = PendingChangeEntity.STATUS_SENDING))
            val path = pathFor(change)
            try {
                messageClient.sendMessage(nodeId, path, change.payload.toByteArray()).await()
                dao.delete(change)
                sent++
                Timber.d("Flushed %s id=%s → %s", change.entityType, change.entityId, path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Flush FAILED %s id=%s", change.entityType, change.entityId)
                handleFailure(change)
                failed++
            }
        }

        return FlushResult(sent, failed)
    }

    /**
     * Full sweep: flush PENDING items, then re-attempt FAILED items
     * whose retry budget is not yet exhausted.
     */
    suspend fun flushAllPending(): FlushResult {
        val pendingResult = flush()
        val retryable = dao.getRetryableFailed()

        if (retryable.isEmpty()) return pendingResult

        var recovered = 0
        var stillFailed = 0

        for (change in retryable) {
            val backoffMs = backoffDelay(change.retryCount)
            delay(backoffMs)

            dao.update(change.copy(status = PendingChangeEntity.STATUS_SENDING))
            val path = pathFor(change)
            try {
                messageClient.sendMessage(nodeId, path, change.payload.toByteArray()).await()
                dao.delete(change)
                recovered++
                Timber.d("Recovered %s id=%s → %s", change.entityType, change.entityId, path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Retry FAILED %s id=%s", change.entityType, change.entityId)
                handleFailure(change)
                stillFailed++
            }
        }

        return FlushResult(
            sent = pendingResult.sent + recovered,
            failed = pendingResult.failed + stillFailed
        )
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /** Total items currently in the queue (any status). */
    suspend fun size(): Int = dao.count()

    /** Number of permanently-FAILED items. */
    suspend fun failedCount(): Int = dao.countFailed()

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun pathFor(change: PendingChangeEntity): String =
        SYNC_PATH_FORMAT.format(Locale.ROOT, change.entityType, change.operationType.lowercase(Locale.ROOT))

    private fun backoffDelay(retryCount: Int): Long =
        BACKOFF_BASE_MS * (1L shl retryCount)

    private suspend fun handleFailure(change: PendingChangeEntity) {
        val nextRetry = change.retryCount + 1
        if (nextRetry >= MAX_RETRIES) {
            Timber.w("Change %s/%s exceeded max retries – marking FAILED",
                change.entityType, change.entityId)
            dao.update(
                change.copy(
                    retryCount = nextRetry,
                    status = PendingChangeEntity.STATUS_FAILED
                )
            )
        } else {
            dao.update(
                change.copy(
                    retryCount = nextRetry,
                    status = PendingChangeEntity.STATUS_PENDING
                )
            )
        }
    }

    // ------------------------------------------------------------------
    // Data classes
    // ------------------------------------------------------------------

    data class FlushResult(
        val sent: Int,
        val failed: Int
    ) {
        val total: Int get() = sent + failed
    }
}
