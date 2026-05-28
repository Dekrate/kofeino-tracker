package pl.dekrate.kofeino.tracker.data.sync.adb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.sync.adb.AdbSyncConfig
import pl.dekrate.kofeino.common.sync.adb.AdbSyncProtocol
import timber.log.Timber
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbSyncServer @Inject constructor(
    private val adbMessageClient: AdbMessageClient,
    private val adbCapabilityClient: AdbCapabilityClient
) : AdbSyncTransport {

    @Suppress("InjectDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var clientSocket: Socket? = null
    @Volatile override var isConnected: Boolean = false
        private set

    /** Auto-start server on construction (debug builds only). */
    init {
        scope.launch { run() }
    }

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun run() {
        try {
            serverSocket = ServerSocket(AdbSyncConfig.PORT).also {
                it.reuseAddress = true
                Timber.i("AdbSyncServer: listening on port %d", AdbSyncConfig.PORT)
            }
            while (coroutineContext.isActive) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    socket.soTimeout = AdbSyncConfig.READ_TIMEOUT_MS.toInt()
                    handleConnection(socket)
                } catch (_: java.net.SocketTimeoutException) {
                    // Timeout is expected — loop back to check isActive
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "AdbSyncServer: fatal error")
        }
    }

    private suspend fun handleConnection(socket: Socket) {
        clientSocket = socket
        isConnected = true
        adbCapabilityClient.setConnected(true)
        adbMessageClient.transport = this
        Timber.i("AdbSyncServer: client connected from %s", socket.remoteSocketAddress)

        try {
            val input = socket.getInputStream()
            val buf = ByteArray(AdbSyncConfig.BUFFER_SIZE)
            var acc = ByteArray(0)

            while (coroutineContext.isActive && socket.isConnected && !socket.isClosed) {
                val n = input.read(buf)
                if (n == -1) break
                acc = acc + buf.copyOfRange(0, n)
                val (frames, rem) = AdbSyncProtocol.parseFrames(acc)
                acc = rem
                for ((path, payload) in frames) {
                    adbMessageClient.dispatchMessage(path, payload, AdbCapabilityClient.ADB_NODE_ID)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            if (coroutineContext.isActive) Timber.d(e, "AdbSyncServer: connection error")
        } finally {
            cleanup()
        }
    }

    @Synchronized
    override fun write(data: ByteArray) {
        @Suppress("UseCheckOrError")
        val sock = clientSocket ?: throw IllegalStateException("AdbSyncServer: not connected")
        sock.getOutputStream().run { write(data); flush() }
    }

    fun stop() {
        Timber.i("AdbSyncServer: stopping")
        cleanup()
        serverSocket?.close()
        serverSocket = null
    }

    private fun cleanup() {
        isConnected = false
        adbCapabilityClient.setConnected(false)
        if (adbMessageClient.transport === this) adbMessageClient.transport = null
        try {
            clientSocket?.close()
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
        }
        clientSocket = null
    }

    fun cancel() {
        stop()
        scope.cancel()
    }
}
