package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.domain.model.TileConfig
import pl.dekrate.kofeino.common.sync.SyncPaths
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
                when (event.type) {
                    DataEvent.TYPE_CHANGED -> Timber.d("DataItem changed: $uri")
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
