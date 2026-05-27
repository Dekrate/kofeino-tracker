package pl.dekrate.kofeino.common.sync.adb

/**
 * Configuration constants for the ADB TCP sync bridge.
 *
 * Used by both phone and watch modules to ensure consistent
 * transport parameters.
 */
object AdbSyncConfig {
    // ——— TCP constants ———
    
    /** Default TCP port for the ADB sync bridge. Must match on both devices. */
    const val PORT = 8888
    
    /** Maximum size of a single frame payload (256KB). */
    const val MAX_PAYLOAD_SIZE = 256 * 1024
    
    /** Maximum size of a single frame path (256 bytes). */
    const val MAX_PATH_SIZE = 256
    
    /** Buffer size for reading from TCP socket (64KB). */
    const val BUFFER_SIZE = 64 * 1024

    // ——— Connection lifecycle ———
    
    /** Connection timeout in milliseconds (15s). */
    const val CONNECT_TIMEOUT_MS = 15_000L
    
    /** Socket read timeout in milliseconds (30s). */
    const val READ_TIMEOUT_MS = 30_000L

    // ——— Reconnection (exponential backoff) ———
    
    /** Initial reconnect delay in milliseconds. */
    const val RECONNECT_BASE_DELAY_MS = 1_000L
    
    /** Maximum reconnect delay in milliseconds. */
    const val RECONNECT_MAX_DELAY_MS = 8_000L
    
    /** Maximum number of reconnection attempts before giving up. */
    const val MAX_RECONNECT_ATTEMPTS = 5

    // ——— ADB forward/reverse ———
    
    /** Localhost address for ADB reverse/forward tunneling. */
    const val ADB_HOST = "127.0.0.1"
}
