package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time sync service that propagates local watch mutations to the paired
 * phone immediately via Wearable MessageAPI (wear module).
 *
 * ## How it works
 * 1. **Enqueue** — the change is written to [PendingSyncQueue] so it survives
 *    transient failures and will be re-attempted by periodic flush.
 * 2. **Send** — the change is sent immediately via [MessageClient.sendMessage]
 *    to the first reachable node with our `caffeine_sync` capability.
 *
 * If no paired device is reachable the send is silently skipped — the queued
 * change will be delivered later by [PendingSyncQueue.flush].
 *
 * ⚠ Mirror of [pl.dekrate.kofeino.tracker.data.sync.RealTimeSyncService].
 */
@Singleton
class RealTimeSyncService @Inject constructor(
    private val pendingSyncQueue: PendingSyncQueue,
    private val messageClient: MessageClient,
    private val capabilityClient: CapabilityClient
) {
    companion object {
        /** Wear capability name used to discover the paired device's sync node. */
        const val SYNC_CAPABILITY_NAME = "caffeine_sync"

        /** Message path format:   /sync/<entityType>/<operationType>  */
        private const val SYNC_PATH_FORMAT = "/sync/%s/%s"
    }

    /**
     * Propagate a single mutation to the paired device.
     *
     * @param entityType  Logical entity type — e.g. `"intake"`, `"drink"`.
     * @param entityId    Stable identifier for the entity (as string).
     * @param operationType Operation — `"INSERT"`, `"UPDATE"`, or `"DELETE"`.
     * @param payload     JSON-serialised entity data (via [SyncPayloadSerializer]).
     */
    suspend fun propagateChange(
        entityType: String,
        entityId: String,
        operationType: String,
        payload: String
    ) {
        // 1. Persist for reliability — survives app restarts
        pendingSyncQueue.enqueue(entityType, entityId, operationType, payload)

        // 2. Attempt immediate real-time delivery
        try {
            val nodeId = resolveNodeId() ?: return
            val path = SYNC_PATH_FORMAT.format(
                Locale.ROOT,
                entityType,
                operationType.lowercase(Locale.ROOT)
            )
            messageClient.sendMessage(nodeId, path, payload.toByteArray()).await()
            Timber.d("Real-time sync sent: %s id=%s via node=%s", path, entityId, nodeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.util.concurrent.ExecutionException) {
            Timber.w(e, "Real-time send failed (queued for retry): %s/%s id=%s",
                entityType, operationType, entityId)
        }
    }

    /**
     * Resolve the first reachable node that exposes our sync capability.
     *
     * @return The node ID, or `null` if no paired device is reachable.
     */
    private suspend fun resolveNodeId(): String? {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(SYNC_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.firstOrNull()?.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.util.concurrent.ExecutionException) {
            Timber.w(e, "Failed to resolve sync node via capability '%s'",
                SYNC_CAPABILITY_NAME)
            null
        }
    }
}
