package pl.dekrate.kofeino.tracker.data.sync.adb

/**
 * Abstraction for the ADB TCP transport connection.
 * Implemented by AdbSyncServer on the phone side.
 */
interface AdbSyncTransport {
    val isConnected: Boolean
    fun write(data: ByteArray)
}
