package pl.dekrate.kofeino.data.sync.adb

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import pl.dekrate.kofeino.common.sync.adb.AdbSyncProtocol
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("CAST_NEVER_SUCCEEDS")
@Singleton
class AdbMessageClient @Inject constructor() : MessageClient(null as Context, null as GoogleApi.Settings) {

    private val listeners = CopyOnWriteArrayList<MessageClient.OnMessageReceivedListener>()
    internal var transport: AdbSyncTransport? = null

    override fun sendMessage(nodeId: String, path: String, data: ByteArray?): Task<Int> {
        return try {
            val t = transport
            if (t == null || !t.isConnected || data == null) {
                Timber.w("AdbMessageClient: no transport or null data, dropping %s", path)
                return Tasks.forResult(0)
            }
            t.write(AdbSyncProtocol.frame(path, data))
            Tasks.forResult(data.size)
        } catch (e: Exception) {
            Tasks.forException(e)
        }
    }

    override fun addListener(listener: MessageClient.OnMessageReceivedListener): Task<Void> {
        listeners.add(listener); return Tasks.forResult(null)
    }

    override fun removeListener(listener: MessageClient.OnMessageReceivedListener): Task<Boolean> {
        listeners.remove(listener); return Tasks.forResult(true)
    }

    override fun addListener(
        listener: MessageClient.OnMessageReceivedListener,
        uri: Uri,
        strategy: Int
    ): Task<Void> = addListener(listener)

    override fun addRpcService(service: MessageClient.RpcService, path: String): Task<Void> =
        Tasks.forResult(null)

    override fun addRpcService(service: MessageClient.RpcService, path: String, microAppId: String): Task<Void> =
        Tasks.forResult(null)

    override fun removeRpcService(service: MessageClient.RpcService): Task<Boolean> =
        Tasks.forResult(false)

    override fun sendRequest(nodeId: String, path: String, data: ByteArray?): Task<ByteArray> =
        Tasks.forException(UnsupportedOperationException("sendRequest not supported in ADB bridge"))

    internal fun dispatchMessage(path: String, payload: ByteArray, sourceNodeId: String) {
        val event = object : MessageEvent {
            override fun getRequestId() = 0
            override fun getPath() = path
            override fun getData() = payload
            override fun getSourceNodeId() = sourceNodeId
        }
        for (listener in listeners) {
            try {
                listener.onMessageReceived(event)
            } catch (e: Exception) {
                Timber.w(e, "AdbMessageClient: listener threw")
            }
        }
    }
}
