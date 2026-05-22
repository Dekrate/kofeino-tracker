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
    private val incomingSyncProcessor: IncomingSyncProcessor
) {
    companion object {
        const val SYNC_CAPABILITY_NAME = "caffeine_sync"
        private const val SYNC_PATH_PREFIX = "/sync"
    }

    private var dataListener: DataClient.OnDataChangedListener? = null
    private var messageListener: MessageClient.OnMessageReceivedListener? = null
    private var capabilityListener: CapabilityClient.OnCapabilityChangedListener? = null

    /** Background scope for processing incoming sync messages on binder threads. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        if (path.startsWith(SYNC_PATH_PREFIX)) {
            Timber.d("Sync message from=$sourceNodeId path=$path payload=${payloadSize}B")
            scope.launch {
                val result = incomingSyncProcessor.processIncoming(messageEvent)
                Timber.d("Incoming sync processed: %s", result)
            }
        } else {
            Timber.d("Non-sync message from=$sourceNodeId path=$path payload=${payloadSize}B")
        }
    }

    private fun handleCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val nodeCount = capabilityInfo.nodes.size
        val nodeIds = capabilityInfo.nodes.map { it.id }
        Timber.d("Capability '${capabilityInfo.name}': $nodeCount node(s) available — $nodeIds")
    }

    @Suppress("TooGenericExceptionCaught")
    fun register() {
        if (isRegistered) return

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

        isRegistered = false
        dataListener = null
        messageListener = null
        capabilityListener = null
        scope.cancel()
        Timber.i("Wearable DataLayer unregistration complete: $successCount unregistered, $failureCount failed")
    }
}
