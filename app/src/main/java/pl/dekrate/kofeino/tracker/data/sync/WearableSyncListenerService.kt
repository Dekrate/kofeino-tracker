package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.sync.SyncPaths
import timber.log.Timber
import javax.inject.Inject

/**
 * Manifest-declared WearableListenerService that receives Wearable Data Layer
 * messages even when the app process is not running.
 *
 * This is required because on Samsung devices, GMS may fail to deliver messages
 * to the running app process due to stale process registrations in the app freezer.
 * A manifest-declared listener service ensures GMS can instantiate the service
 * on demand to handle incoming messages.
 */
@AndroidEntryPoint
class WearableSyncListenerService : WearableListenerService() {

    @Inject
    lateinit var incomingSyncProcessor: IncomingSyncProcessor

    @Inject
    lateinit var fullSyncManager: FullSyncManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Timber.d("WearableSyncListenerService: received message path=%s from=%s",
            messageEvent.path, messageEvent.sourceNodeId)

        scope.launch {
            try {
                when (messageEvent.path) {
                    SyncPaths.MESSAGE_FULL_SYNC_REQUEST -> {
                        fullSyncManager.handleFullSyncRequest(messageEvent)
                    }
                    SyncPaths.MESSAGE_FULL_SYNC_RESPONSE -> {
                        fullSyncManager.handleFullSyncResponse(messageEvent)
                    }
                    else -> {
                        incomingSyncProcessor.processIncoming(messageEvent)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "WearableSyncListenerService: error handling message path=%s", messageEvent.path)
            }
        }
    }

    override fun onDestroy() {
        Timber.d("WearableSyncListenerService: destroyed")
        super.onDestroy()
    }
}
