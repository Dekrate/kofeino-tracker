package pl.dekrate.kofeino.data.sync.adb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.common.sync.adb.AdbSyncConfig
import pl.dekrate.kofeino.common.sync.adb.AdbSyncProtocol
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbSyncClient @Inject constructor(
    private val adbMessageClient: AdbMessageClient,
    private val adbCapabilityClient: AdbCapabilityClient
) : AdbSyncTransport {

    @Suppress("InjectDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var socket: Socket? = null
    @Volatile override var isConnected: Boolean = false
        private set

    /** Auto-start connecting on construction (debug builds only). */
    init {
        scope.launch { run() }
    }

    @Suppress("SuspendFunSwallowedCancellation", "LoopWithTooManyJumpStatements")
    private suspend fun run() {
        var attempt = 0
        while (coroutineContext.isActive) {
            try {
                attempt++
                val s = Socket().apply {
                    connect(
                        InetSocketAddress(AdbSyncConfig.ADB_HOST, AdbSyncConfig.PORT),
                        AdbSyncConfig.CONNECT_TIMEOUT_MS.toInt()
                    )
                    soTimeout = AdbSyncConfig.READ_TIMEOUT_MS.toInt()
                }
                socket = s
                isConnected = true
                adbCapabilityClient.setConnected(true)
                adbMessageClient.transport = this
                attempt = 0
                Timber.i(
                    "AdbSyncClient: connected to %s:%d",
                    AdbSyncConfig.ADB_HOST,
                    AdbSyncConfig.PORT
                )
                readLoop(s)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                if (!coroutineContext.isActive) break
                Timber.d(e, "AdbSyncClient: attempt %d failed", attempt)
                cleanup()
                if (attempt < AdbSyncConfig.MAX_RECONNECT_ATTEMPTS) {
                    val delayMs = (AdbSyncConfig.RECONNECT_BASE_DELAY_MS shl (attempt - 1).coerceIn(0, 31))
                        .coerceAtMost(AdbSyncConfig.RECONNECT_MAX_DELAY_MS)
                    delay(delayMs)
                } else {
                    Timber.w("AdbSyncClient: max attempts reached")
                    break
                }
            }
        }
    }

    private suspend fun readLoop(socket: Socket) {
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
            if (coroutineContext.isActive) Timber.d(e, "AdbSyncClient: read error")
        } finally {
            cleanup()
        }
    }

    @Synchronized
    override fun write(data: ByteArray) {
        @Suppress("UseCheckOrError")
        val s = socket ?: throw IllegalStateException("AdbSyncClient: not connected")
        s.getOutputStream().run { write(data); flush() }
    }

    fun stop() {
        Timber.i("AdbSyncClient: stopping")
        cleanup()
        socket = null
    }

    private fun cleanup() {
        isConnected = false
        adbCapabilityClient.setConnected(false)
        if (adbMessageClient.transport === this) adbMessageClient.transport = null
        try {
            socket?.close()
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
        }
        socket = null
    }

    fun cancel() {
        stop()
        scope.cancel()
    }
}
