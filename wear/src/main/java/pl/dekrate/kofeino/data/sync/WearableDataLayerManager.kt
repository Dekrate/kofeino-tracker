package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.domain.model.TileConfig
import pl.dekrate.kofeino.common.sync.SyncPaths
import pl.dekrate.kofeino.common.sync.SyncSessionState
import pl.dekrate.kofeino.data.local.TileDataStorePreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of Wearable Data Layer listeners for cross-device sync
 * on the Wear OS module.
 *
 * All registrations are wrapped in try-catch so that failure degrades
 * gracefully with only a log warning.
 *
 * ## Design notes
 * - **SOLID**: single responsibility — lifecycle of Wearable listeners only.
 * - **Standalone safe**: if no paired device exists, listeners simply never fire.
 */
@Singleton
class WearableDataLayerManager @Inject constructor(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val capabilityClient: CapabilityClient,
    private val nodeClient: NodeClient,
    private val incomingSyncProcessor: IncomingSyncProcessor,
    private val syncStatusTracker: SyncStatusTracker,
    private val fullSyncManager: FullSyncManager,
    private val tileDataStorePreferences: TileDataStorePreferences
) {
    companion object {
        const val SYNC_CAPABILITY_NAME = "caffeine_sync"
        private const val SYNC_PATH_PREFIX = "/sync"
    }

    private var dataListener: DataClient.OnDataChangedListener? = null
    private var messageListener: MessageClient.OnMessageReceivedListener? = null
    private var capabilityListener: CapabilityClient.OnCapabilityChangedListener? = null

    /** Background scope for processing incoming sync messages on binder threads. */
    @Suppress("InjectDispatcher")
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tracks whether listeners are currently registered (idempotency guard). */
    private var isRegistered: Boolean = false

    /**
     * Cached local node ID, used to filter out self-created DataItems
     * so we do not process our own FullSync messages as incoming.
     */
    @Volatile
    private var localNodeId: String? = null

    /**
     * Handles data item changes from the paired device.
     *
     * **Thread safety:** This callback may fire on a binder thread. Do not add
     * blocking operations or long-running work without switching to a
     * background dispatcher.
     *
     * **Note:** Data item URIs are logged for observability. In production,
     * consider logging only the path segment if URIs may contain user identifiers.
     */
    private fun handleDataChanged(dataEventBuffer: DataEventBuffer) {
        try {
            for (event in dataEventBuffer) {
                val uri = event.dataItem.uri
                val sourceNodeId = uri.authority
                when (event.type) {
                    DataEvent.TYPE_CHANGED -> {
                        val path = uri.path
                        val data = event.dataItem.data
                        Timber.d("DataItem changed: $uri (${data?.size ?: 0}B)")

                        // Skip DataItems originating from this device to avoid
                        // processing our own FullSync messages.
                        if (sourceNodeId != null && sourceNodeId == localNodeId) continue

                        // Route FullSync DataItems to FullSyncManager
                        when {
                            path?.startsWith(SyncPaths.DATA_ITEM_FULL_SYNC_REQUEST) == true -> {
                                if (data != null) {
                                    val messageEvent = object : MessageEvent {
                                        override fun getRequestId() = 0
                                        override fun getPath() = SyncPaths.MESSAGE_FULL_SYNC_REQUEST
                                        override fun getData() = data
                                        override fun getSourceNodeId() = sourceNodeId ?: ""
                                        override fun toString() =
                                            "DataItemRequest{path=$path, size=${data.size}}"
                                    }
                                    scope.launch { fullSyncManager.handleFullSyncRequest(messageEvent) }
                                }
                            }
                            path?.startsWith(SyncPaths.DATA_ITEM_FULL_SYNC_RESPONSE) == true -> {
                                if (data != null) {
                                    val messageEvent = object : MessageEvent {
                                        override fun getRequestId() = 0
                                        override fun getPath() = SyncPaths.MESSAGE_FULL_SYNC_RESPONSE
                                        override fun getData() = data
                                        override fun getSourceNodeId() = sourceNodeId ?: ""
                                        override fun toString() =
                                            "DataItemResponse{path=$path, size=${data.size}}"
                                    }
                                    scope.launch { fullSyncManager.handleFullSyncResponse(messageEvent) }
                                }
                            }
                            else -> { /* unrecognised DataItem — ignore */ }
                        }
                    }
                    DataEvent.TYPE_DELETED -> Timber.d("DataItem deleted: $uri")
                    else -> Timber.d("DataItem unknown event type=${event.type}: $uri")
                }
            }
        } finally {
            dataEventBuffer.release()
        }
    }

    private fun handleMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val sourceNodeId = messageEvent.sourceNodeId
        val payloadSize = messageEvent.data.size

        // Route full-sync messages to FullSyncManager
        when (path) {
            SyncPaths.MESSAGE_FULL_SYNC_REQUEST -> {
                Timber.d("FullSync request from=%s", sourceNodeId)
                scope.launch { fullSyncManager.handleFullSyncRequest(messageEvent) }
                return
            }
            SyncPaths.MESSAGE_FULL_SYNC_RESPONSE -> {
                Timber.d("FullSync response from=%s (%dB)", sourceNodeId, payloadSize)
                scope.launch { fullSyncManager.handleFullSyncResponse(messageEvent) }
                return
            }
        }

        // Route tile config messages to TileDataStorePreferences
        if (path == SyncPaths.MESSAGE_TILE_CONFIG_CHANGED) {
            Timber.d("Tile config message from=%s payload=%dB", sourceNodeId, payloadSize)
            scope.launch {
                val payload = messageEvent.data.toString(Charsets.UTF_8)
                val config = TileConfig.fromMessagePayload(payload)
                tileDataStorePreferences.setTileConfig(config)
                Timber.d("Tile config updated from message: %s", config.displayOption.name)
            }
            return
        }

        if (path.startsWith(SYNC_PATH_PREFIX)) {
            Timber.d("Sync message from=%s path=%s payload=%dB", sourceNodeId, path, payloadSize)
            scope.launch {
                val result = incomingSyncProcessor.processIncoming(messageEvent)
                Timber.d("Incoming sync processed: %s", result)
            }
        } else {
            Timber.d("Non-sync message from=%s path=%s payload=%dB", sourceNodeId, path, payloadSize)
        }
    }

    private fun handleCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val nodeCount = capabilityInfo.nodes.size
        val nodeIds = capabilityInfo.nodes.map { it.id }
        Timber.d("Capability '${capabilityInfo.name}': $nodeCount node(s) available — $nodeIds")
        syncStatusTracker.onDeviceConnectionChanged(nodeCount > 0)

        // Trigger full sync for each connected node
        for (node in capabilityInfo.nodes) {
            if (node.isNearby) {
                Timber.d("FullSync: triggering for nearby node=%s", node.id)
                fullSyncManager.onNodeConnected(node.id)
            }
        }

        // If no nodes remain connected, reset any active sync
        if (capabilityInfo.nodes.isEmpty()) {
            fullSyncManager.onNodeDisconnected("")
        }
    }

    @Suppress("InjectDispatcher", "TooGenericExceptionCaught")
    fun register() {
        if (isRegistered) return
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        var successCount = 0
        var failureCount = 0

        dataListener = DataClient.OnDataChangedListener { buffer: DataEventBuffer ->
            handleDataChanged(buffer)
        }
        try {
            dataListener?.let { dataClient.addListener(it) }
            Timber.d("DataClient.OnDataChangedListener registered")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to register DataClient listener")
            failureCount++
            dataListener = null
        }

        messageListener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            handleMessageReceived(event)
        }
        try {
            messageListener?.let { messageClient.addListener(it) }
            Timber.d("MessageClient.OnMessageReceivedListener registered")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to register MessageClient listener")
            failureCount++
            messageListener = null
        }

        capabilityListener = CapabilityClient.OnCapabilityChangedListener { info: CapabilityInfo ->
            handleCapabilityChanged(info)
        }
        try {
            capabilityListener?.let { capabilityClient.addListener(it, SYNC_CAPABILITY_NAME) }
            Timber.d("CapabilityClient.OnCapabilityChangedListener registered for '$SYNC_CAPABILITY_NAME'")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to register CapabilityClient listener")
            failureCount++
            capabilityListener = null
        }

        try {
            capabilityClient.addLocalCapability(SYNC_CAPABILITY_NAME)
            Timber.d("Local capability '$SYNC_CAPABILITY_NAME' published")
        } catch (e: Exception) {
            Timber.w(e, "Failed to publish local capability '$SYNC_CAPABILITY_NAME'")
        }

        isRegistered = successCount > 0

        // Cache the local node ID so we can filter self-created DataItems
        // and avoid processing our own FullSync messages as incoming.
        scope.launch {
            localNodeId = try {
                nodeClient.localNode.await().id
            } catch (e: Exception) {
                Timber.w(e, "Failed to get local node ID")
                null
            }
        }

        // Fallback: Check connected nodes directly via NodeClient when capability
        // discovery is unavailable (e.g., Samsung bridge doesn't propagate custom capabilities).
        if (isRegistered) {
            scope.launch {
                try {
                    kotlinx.coroutines.delay(3000) // short delay for GMS to settle
                    val nodes = nodeClient.connectedNodes.await()
                    if (nodes.isNotEmpty()) {
                        Timber.d("Fallback: %d connected node(s) found via NodeClient — simulating capability discovery", nodes.size)
                        val capabilityInfo = object : CapabilityInfo {
                            override fun getName(): String = SYNC_CAPABILITY_NAME
                            override fun getNodes(): Set<Node> = nodes.toSet()
                        }
                        handleCapabilityChanged(capabilityInfo)
                    } else {
                        Timber.d("Fallback: 0 connected nodes (will retry on capability listener)")
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Timber.w(e, "Fallback: node check failed")
                }
            }
        }

        // Periodic poll every 30 seconds as a safety net — also resets stale
        // AwaitingResponse sessions so FullSync can retry when GMS delivery fails.
        scope.launch {
            try {
                kotlinx.coroutines.delay(30000)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isNotEmpty()) {
                    // Reset any stale AwaitingResponse session before re-triggering
                    for (node in nodes) {
                        fullSyncManager.checkAndResetStaleSession(node.id)
                    }
                    Timber.d("Fallback retry: %d connected node(s) via NodeClient", nodes.size)
                    val capabilityInfo = object : CapabilityInfo {
                        override fun getName(): String = SYNC_CAPABILITY_NAME
                        override fun getNodes(): Set<Node> = nodes.toSet()
                    }
                    handleCapabilityChanged(capabilityInfo)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.w(e, "Fallback retry: node check failed")
            }
        }
        Timber.i("Wearable DataLayer registration complete: $successCount registered, $failureCount failed")
    }

    @Suppress("TooGenericExceptionCaught")
    fun unregister() {
        if (!isRegistered) return

        var successCount = 0
        var failureCount = 0

        try {
            dataListener?.let { dataClient.removeListener(it) }
            Timber.d("DataClient.OnDataChangedListener unregistered")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister DataClient listener")
            failureCount++
        }

        try {
            messageListener?.let { messageClient.removeListener(it) }
            Timber.d("MessageClient.OnMessageReceivedListener unregistered")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister MessageClient listener")
            failureCount++
        }

        try {
            capabilityListener?.let { capabilityClient.removeListener(it) }
            Timber.d("CapabilityClient.OnCapabilityChangedListener unregistered")
            successCount++
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister CapabilityClient listener")
            failureCount++
        }

        try {
            capabilityClient.removeLocalCapability(SYNC_CAPABILITY_NAME)
            Timber.d("Local capability '$SYNC_CAPABILITY_NAME' unpublished")
        } catch (e: Exception) {
            Timber.w(e, "Failed to unpublish local capability '$SYNC_CAPABILITY_NAME'")
        }

        isRegistered = false
        dataListener = null
        messageListener = null
        capabilityListener = null
        scope.cancel()
        Timber.i("Wearable DataLayer unregistration complete: $successCount unregistered, $failureCount failed")
    }
}
