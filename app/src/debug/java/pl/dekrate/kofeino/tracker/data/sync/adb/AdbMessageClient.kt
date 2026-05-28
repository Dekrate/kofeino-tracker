package pl.dekrate.kofeino.tracker.data.sync.adb

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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbMessageClient @Inject constructor(
    @ApplicationContext context: Context
) : MessageClient(context, GoogleApi.Settings.Builder().build()) {

    private val listeners = CopyOnWriteArrayList<MessageClient.OnMessageReceivedListener>()
    internal var transport: AdbSyncTransport? = null

    override fun sendMessage(nodeId: String, path: String, data: ByteArray?): Task<Int> {
        return try {
            val t = transport
            if (t == null || !t.isConnected || data == null) {
                Timber.w("AdbMessageClient: no transport or null data, dropping %s", path)
                return Tasks.forResult(0)
            }
            val frame = AdbSyncProtocol.frame(path, data)
            t.write(frame)
            Tasks.forResult(data.size)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.w(e, "AdbMessageClient: send failed for %s", path)
            Tasks.forException(e)
        }
    }

    @Suppress("ForbiddenVoid")
    override fun addListener(listener: MessageClient.OnMessageReceivedListener): Task<Void> {
        listeners.add(listener); return Tasks.forResult(null)
    }

    override fun removeListener(listener: MessageClient.OnMessageReceivedListener): Task<Boolean> {
        listeners.remove(listener); return Tasks.forResult(true)
    }

    @Suppress("ForbiddenVoid")
    override fun addListener(
        listener: MessageClient.OnMessageReceivedListener,
        uri: Uri,
        strategy: Int
    ): Task<Void> = addListener(listener)

    @Suppress("ForbiddenVoid")
    override fun addRpcService(service: MessageClient.RpcService, path: String): Task<Void> =
        Tasks.forResult(null)

    @Suppress("ForbiddenVoid")
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
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.w(e, "AdbMessageClient: listener threw")
            }
        }
    }
}
