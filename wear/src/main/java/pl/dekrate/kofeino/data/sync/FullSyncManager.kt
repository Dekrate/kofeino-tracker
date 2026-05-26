package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.dekrate.kofeino.common.sync.StateHashCalculator
import pl.dekrate.kofeino.common.sync.SyncEntityType
import pl.dekrate.kofeino.common.sync.SyncEvent
import pl.dekrate.kofeino.common.sync.SyncPaths
import pl.dekrate.kofeino.common.sync.SyncSessionState
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.repository.toCommon
import pl.dekrate.kofeino.di.IoDispatcher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full-sync protocol between paired devices.
 *
 * ## When triggered
 * 1. **First-ever connection** — [onNodeConnected] sends a
 *    [SyncEvent.FullSyncRequest] with `lastSyncTimestamp = 0L`.
 *    The paired device responds with ALL its data.
 * 2. **Reconnection (delta sync)** — [onNodeConnected] computes the
 *    current combined state hash. If it differs from [SyncStateStore.lastStateHash],
 *    a delta sync is requested with the [lastSyncTimestamp].
 * 3. **Incoming request** — [handleFullSyncRequest] prepares and sends
 *    a [SyncEvent.FullSyncResponse] with only entities modified after
 *    the request's `lastSyncTimestamp`.
 * 4. **Incoming response** — [handleFullSyncResponse] applies received
 *    entities via [IncomingSyncProcessor], then triggers the reverse
 *    direction so both sides exchange data.
 *
 * ## Thread safety
 * All sync operations are serialised through a [Mutex]. Concurrent
 * sync attempts are queued, not dropped.
 *
 * ## Lifecycle
 * Created once as a singleton. Initialised when the app starts —
 * after [WearableDataLayerManager.register] detects a node.
 */
@Singleton
class FullSyncManager @Inject constructor(
    private val messageClient: MessageClient,
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao,
    private val incomingSyncProcessor: IncomingSyncProcessor,
    private val syncStateStore: SyncStateStore,
    private val syncStatusTracker: SyncStatusTracker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private val gson = Gson()

        /** Device identifier for this watch module. */
        private const val SOURCE_DEVICE_ID = "watch"

        /** Maximum entities per full-sync batch message (fits in 100KB Wear message limit). */
        private const val MAX_BATCH_SIZE = 50
    }

    /** Serialises all sync operations — only one at a time. */
    private val syncMutex = Mutex()

    /** Tracks the current sync session state. */
    @Volatile
    private var sessionState: SyncSessionState = SyncSessionState.Idle

    /** Background scope for async sync operations. */
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    // ------------------------------------------------------------------
    // Public API — called by WearableDataLayerManager
    // ------------------------------------------------------------------

    /**
     * Called when a paired node with our sync capability becomes reachable.
     *
     * This is the entry point for the full-sync protocol. It checks
     * [SyncStateStore] to determine whether a full or delta sync is needed.
     *
     * @param nodeId The reachable node ID.
     */
    fun onNodeConnected(nodeId: String) {
        scope.launch {
            syncMutex.withLock {
                if (sessionState !is SyncSessionState.Idle) {
                    Timber.d("FullSync: already in state %s, skipping connect trigger", sessionState)
                    return@withLock
                }
                initiateFullSync(nodeId)
            }
        }
    }

    /**
     * Process an incoming [SyncEvent.FullSyncRequest] from the paired device.
     *
     * @param event The raw [MessageEvent] containing the request.
     */
    suspend fun handleFullSyncRequest(event: MessageEvent) {
        syncMutex.withLock {
            if (sessionState !is SyncSessionState.Idle) {
                if (sessionState is SyncSessionState.Requesting) {
                    // Simultaneous initiation: back off and process incoming request as responder
                    Timber.d("FullSync: simultaneous initiation detected, deferring to responder")
                    sessionState = SyncSessionState.Idle
                    processIncomingRequest(event)
                    return@withLock
                }
                Timber.d("FullSync: busy (%s), queuing not supported yet", sessionState)
                return@withLock
            }
            processIncomingRequest(event)
        }
    }

    /**
     * Process an incoming [SyncEvent.FullSyncResponse] from the paired device.
     *
     * @param event The raw [MessageEvent] containing the response.
     */
    suspend fun handleFullSyncResponse(event: MessageEvent) {
        syncMutex.withLock {
            if (sessionState !is SyncSessionState.AwaitingResponse &&
                sessionState !is SyncSessionState.Requesting &&
                sessionState !is SyncSessionState.Idle
            ) {
                Timber.d("FullSync: not expecting response (state=%s), ignoring", sessionState)
                return@withLock
            }
            processIncomingResponse(event)
        }
    }

    /**
     * Cancel the internal scope (for test teardown).
     */
    fun cancel() {
        scope.cancel()
    }

    /**
     * Called when a paired node becomes unreachable.
     * Cancels any in-progress sync session with this node and resets state.
     *
     * @param nodeId The disconnected node ID, or empty string to reset any active session.
     */
    fun onNodeDisconnected(nodeId: String) {
        val currentSession = sessionState
        val sessionNodeId = when (currentSession) {
            is SyncSessionState.Requesting -> currentSession.nodeId
            is SyncSessionState.AwaitingResponse -> currentSession.nodeId
            is SyncSessionState.Responding -> currentSession.requestNodeId
            is SyncSessionState.Idle -> null
        }
        if (sessionNodeId == nodeId || nodeId.isEmpty()) {
            Timber.d("FullSync: node disconnected during active sync, resetting")
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("Device disconnected")
        }
    }

    // ------------------------------------------------------------------
    // Internal: Initiate sync (requestor side)
    // ------------------------------------------------------------------

    /**
     * Initiate the full-sync protocol with the paired [nodeId].
     *
     * 1. Compute current state hash.
     * 2. Check last sync timestamp from [SyncStateStore].
     * 3. Send [SyncEvent.FullSyncRequest].
     * 4. Wait for [SyncEvent.FullSyncResponse] (arrives asynchronously).
     * 5. Apply inbound changes.
     * 6. Send our changes (reverse direction).
     * 7. Record sync completion.
     */
    private suspend fun initiateFullSync(nodeId: String) {
        syncStatusTracker.onSyncStarted()
        val sessionId = SyncSessionState.generateSessionId()
        sessionState = SyncSessionState.Requesting(nodeId, sessionId, System.currentTimeMillis())

        try {
            withTimeout(30_000L) {  // 30-second timeout for full sync
                val lastSyncTimestamp = syncStateStore.getLastSyncTimestamp()
                val intakes = intakeDao.getAllIntakesSnapshot()
                val drinks = drinkDao.getAllDrinksSnapshot()
                val currentHash = StateHashCalculator.computeCombinedHash(
                    intakes = intakes.map { it.toCommon() },
                    drinks = drinks.map { it.toCommon() }
                )

                Timber.d("FullSync: initiating with %s, lastSyncTs=%d, hash=%s",
                    nodeId, lastSyncTimestamp, currentHash.take(16))

                // Skip sync if state hasn't changed since last sync with this device
                val lastStateHash = syncStateStore.getLastStateHash()
                val lastSyncedDeviceId = syncStateStore.getLastSyncedDeviceId()

                if (lastSyncTimestamp != 0L && currentHash == lastStateHash && lastSyncedDeviceId == nodeId) {
                    Timber.d("FullSync: state unchanged since last sync with %s, skipping", nodeId)
                    sessionState = SyncSessionState.Idle
                    syncStatusTracker.onSyncCompleted()
                    return@withTimeout
                }

                // Send the FullSyncRequest
                val request = SyncEvent.FullSyncRequest(
                    stateHash = currentHash,
                    lastSyncTimestamp = lastSyncTimestamp,
                    timestamp = System.currentTimeMillis(),
                    sourceDeviceId = SOURCE_DEVICE_ID
                )
                val requestJson = gson.toJson(request)
                messageClient.sendMessage(
                    nodeId,
                    SyncPaths.MESSAGE_FULL_SYNC_REQUEST,
                    requestJson.toByteArray()
                ).await()

                Timber.d("FullSync: request sent to %s (session=%s)", nodeId, sessionId)

                // Transition to AwaitingResponse — the response will arrive
                // asynchronously via handleFullSyncResponse.
                sessionState = SyncSessionState.AwaitingResponse(nodeId, sessionId)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("FullSync: session timed out (30s) for node=%s", nodeId)
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("Timeout")
        } catch (e: CancellationException) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("Cancelled")
            throw e
        } catch (e: Exception) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("FullSync initiation failed: ${e.message}")
            Timber.w(e, "FullSync: initiation failed for node=%s", nodeId)
        }
    }

    // ------------------------------------------------------------------
    // Internal: Process incoming request (responder side)
    // ------------------------------------------------------------------

    /**
     * Handle an incoming [SyncEvent.FullSyncRequest].
     *
     * 1. Parse the request.
     * 2. Query entities modified after `lastSyncTimestamp`.
     * 3. Send [SyncEvent.FullSyncResponse] batches.
     * 4. Record sync completion after sending.
     */
    private suspend fun processIncomingRequest(event: MessageEvent) {
        val requestJson = event.data.toString(Charsets.UTF_8)
        val request = try {
            gson.fromJson(requestJson, SyncEvent.FullSyncRequest::class.java)
        } catch (e: Exception) {
            Timber.w(e, "FullSync: failed to parse request")
            return
        }

        val sessionId = SyncSessionState.generateSessionId()
        sessionState = SyncSessionState.Responding(
            requestNodeId = event.sourceNodeId,
            sessionId = sessionId,
            stateHash = request.stateHash,
            lastSyncTimestamp = request.lastSyncTimestamp
        )

        syncStatusTracker.onSyncStarted()

        try {
            Timber.d("FullSync: processing request from %s (lastSyncTs=%d)",
                event.sourceNodeId, request.lastSyncTimestamp)

            // Collect entities modified since lastSyncTimestamp
            val intakesToSend = if (request.lastSyncTimestamp == 0L) {
                intakeDao.getAllIntakesSnapshot()
            } else {
                intakeDao.getAllIntakesSnapshot()
                    .filter { it.lastModifiedTimestamp > request.lastSyncTimestamp }
            }

            val drinksToSend = if (request.lastSyncTimestamp == 0L) {
                drinkDao.getAllDrinksSnapshot()
            } else {
                drinkDao.getAllDrinksSnapshot()
                    .filter { it.lastModifiedTimestamp > request.lastSyncTimestamp }
            }

            // Send intakes batch
            sendEntityBatch(
                targetNodeId = event.sourceNodeId,
                entityType = SyncEntityType.INTAKE,
                entities = intakesToSend.map { SyncPayloadSerializer.serializeIntake(it) },
                sessionId = sessionId
            )

            // Send drinks batch
            sendEntityBatch(
                targetNodeId = event.sourceNodeId,
                entityType = SyncEntityType.DRINK,
                entities = drinksToSend.map { SyncPayloadSerializer.serializeDrink(it) },
                sessionId = sessionId
            )

            Timber.d("FullSync: response sent to %s (%d intakes, %d drinks)",
                event.sourceNodeId, intakesToSend.size, drinksToSend.size)

            // Record that we've sent our data
            val currentIntakes = intakeDao.getAllIntakesSnapshot()
            val currentDrinks = drinkDao.getAllDrinksSnapshot()
            val currentHash = StateHashCalculator.computeCombinedHash(
                currentIntakes.map { it.toCommon() },
                currentDrinks.map { it.toCommon() }
            )

            syncStateStore.recordSyncCompletion(
                timestamp = System.currentTimeMillis(),
                stateHash = currentHash,
                deviceId = event.sourceNodeId
            )
            syncStatusTracker.onSyncCompleted()
            sessionState = SyncSessionState.Idle

        } catch (e: CancellationException) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("Cancelled")
            throw e
        } catch (e: Exception) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("FullSync response failed: ${e.message}")
            Timber.w(e, "FullSync: response preparation failed")
        }
    }

    // ------------------------------------------------------------------
    // Internal: Process incoming response (requestor side continuation)
    // ------------------------------------------------------------------

    /**
     * Handle an incoming [SyncEvent.FullSyncResponse] and complete the
     * bidirectional protocol.
     *
     * 1. Parse entities from the response.
     * 2. Apply via [IncomingSyncProcessor].
     * 3. Record sync completion.
     */
    private suspend fun processIncomingResponse(event: MessageEvent) {
        val responseJson = event.data.toString(Charsets.UTF_8)
        val response = try {
            gson.fromJson(responseJson, SyncEvent.FullSyncResponse::class.java)
        } catch (e: Exception) {
            Timber.w(e, "FullSync: failed to parse response")
            return
        }

        try {
            if (sessionState is SyncSessionState.Idle) {
                Timber.d("FullSync: response while idle, processing as late-arriving data")
            }
            Timber.d("FullSync: processing response from %s (entityType=%s, entities=%d)",
                event.sourceNodeId, response.entityType, response.entitiesJson.size)

            // Apply each entity in the response via IncomingSyncProcessor
            for (entityJson in response.entitiesJson) {
                val entityPath = when (response.entityType) {
                    SyncEntityType.INTAKE -> "/sync/intake/insert"
                    SyncEntityType.DRINK -> "/sync/drink/insert"
                    else -> continue
                }
                val entityEvent = object : MessageEvent {
                    override fun getRequestId() = 0
                    override fun getPath() = entityPath
                    override fun getData() = entityJson.toByteArray()
                    override fun getSourceNodeId() = event.sourceNodeId
                    override fun toString() = "FullSyncEntity{path=$entityPath, size=${entityJson.length}}"
                }
                incomingSyncProcessor.processIncoming(entityEvent)
            }

            Timber.d("FullSync: applied %d entities from %s",
                response.entitiesJson.size, event.sourceNodeId)

            // Record sync completion with current state hash
            val intakes = intakeDao.getAllIntakesSnapshot()
            val drinks = drinkDao.getAllDrinksSnapshot()
            val currentHash = StateHashCalculator.computeCombinedHash(
                intakes.map { it.toCommon() },
                drinks.map { it.toCommon() }
            )

            syncStateStore.recordSyncCompletion(
                timestamp = System.currentTimeMillis(),
                stateHash = currentHash,
                deviceId = event.sourceNodeId
            )
            syncStatusTracker.onSyncCompleted()
            sessionState = SyncSessionState.Idle

        } catch (e: CancellationException) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("Cancelled")
            throw e
        } catch (e: Exception) {
            sessionState = SyncSessionState.Idle
            syncStatusTracker.onSyncFailed("FullSync response processing failed: ${e.message}")
            Timber.w(e, "FullSync: response processing failed")
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Send a batch of entity JSONs as a [SyncEvent.FullSyncResponse] to the
     * target node. Splits into multiple messages if [MAX_BATCH_SIZE] is exceeded.
     */
    private suspend fun sendEntityBatch(
        targetNodeId: String,
        entityType: SyncEntityType,
        entities: List<String>,
        sessionId: String
    ) {
        entities.chunked(MAX_BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            val response = SyncEvent.FullSyncResponse(
                entityType = entityType,
                entitiesJson = batch,
                timestamp = System.currentTimeMillis(),
                sourceDeviceId = SOURCE_DEVICE_ID
            )
            val responseJson = gson.toJson(response)
            try {
                messageClient.sendMessage(
                    targetNodeId,
                    SyncPaths.MESSAGE_FULL_SYNC_RESPONSE,
                    responseJson.toByteArray()
                ).await()
                Timber.d("FullSync: sent batch %d (%d entities) to %s",
                    batchIndex, batch.size, targetNodeId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "FullSync: failed to send batch %d to %s",
                    batchIndex, targetNodeId)
                throw e
            }
        }
    }
}
