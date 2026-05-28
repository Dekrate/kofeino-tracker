package pl.dekrate.kofeino.data.sync.adb

/**
 * Abstraction for the ADB TCP transport connection.
 * Implemented by AdbSyncClient on the watch side.
 */
interface AdbSyncTransport {
    val isConnected: Boolean
    fun write(data: ByteArray)
}
