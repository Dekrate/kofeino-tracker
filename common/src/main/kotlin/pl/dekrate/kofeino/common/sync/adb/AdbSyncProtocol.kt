package pl.dekrate.kofeino.common.sync.adb

/**
 * Length-prefixed message framing for the ADB TCP sync bridge.
 *
 * Wire format (big-endian):
 *   [4B pathLen][path bytes][4B payloadLen][payload bytes]
 *
 * All lengths are unsigned 32-bit integers.
 *
 * Thread safety: stateless and thread-safe.
 */
@Suppress("MagicNumber")
object AdbSyncProtocol {
    
    private const val LENGTH_FIELD_BYTES = 4
    
    /** Maximum bytes to accumulate in the parsing buffer. */
    const val MAX_BUFFER_BYTES = AdbSyncConfig.MAX_PAYLOAD_SIZE + 
        AdbSyncConfig.MAX_PATH_SIZE + LENGTH_FIELD_BYTES * 2

    /**
     * Frame a message for transmission.
     *
     * @param path The message path (e.g., "/sync/intake/insert").
     * @param payload The binary payload.
     * @return Framed byte array ready to write to a TCP socket.
     * @throws IllegalArgumentException if path or payload exceeds max size.
     */
    fun frame(path: String, payload: ByteArray): ByteArray {
        val pathBytes = path.toByteArray(Charsets.UTF_8)
        require(pathBytes.size <= AdbSyncConfig.MAX_PATH_SIZE) {
            "Path too long: ${pathBytes.size} > ${AdbSyncConfig.MAX_PATH_SIZE}"
        }
        require(payload.size <= AdbSyncConfig.MAX_PAYLOAD_SIZE) {
            "Payload too large: ${payload.size} > ${AdbSyncConfig.MAX_PAYLOAD_SIZE}"
        }
        val buffer = ByteArray(LENGTH_FIELD_BYTES * 2 + pathBytes.size + payload.size)
        var offset = 0
        
        // Write path length (big-endian)
        buffer[offset++] = (pathBytes.size shr 24).toByte()
        buffer[offset++] = (pathBytes.size shr 16).toByte()
        buffer[offset++] = (pathBytes.size shr 8).toByte()
        buffer[offset++] = pathBytes.size.toByte()
        
        // Write path bytes
        pathBytes.copyInto(buffer, offset)
        offset += pathBytes.size
        
        // Write payload length (big-endian)
        buffer[offset++] = (payload.size shr 24).toByte()
        buffer[offset++] = (payload.size shr 16).toByte()
        buffer[offset++] = (payload.size shr 8).toByte()
        buffer[offset++] = payload.size.toByte()
        
        // Write payload bytes
        payload.copyInto(buffer, offset)
        
        return buffer
    }

    /**
     * Try to parse one or more complete frames from [buffer].
     *
     * @param buffer Accumulated bytes (may contain partial frames).
     * @return Pair of (parsed frames, remaining bytes).
     *         Parsed frames are (path, payload) pairs.
     */
    @Suppress("LoopWithTooManyJumpStatements")
    fun parseFrames(buffer: ByteArray): Pair<List<Pair<String, ByteArray>>, ByteArray> {
        if (buffer.size > MAX_BUFFER_BYTES) {
            return emptyList<Pair<String, ByteArray>>() to ByteArray(0)
        }
        val frames = mutableListOf<Pair<String, ByteArray>>()
        var offset = 0
        
        while (offset + LENGTH_FIELD_BYTES <= buffer.size) {
            // Read path length
            val pathLen = readUint32(buffer, offset)
            offset += LENGTH_FIELD_BYTES
            
            if (pathLen > AdbSyncConfig.MAX_PATH_SIZE || pathLen < 0) {
                // Protocol error — discard everything and stop
                return frames to ByteArray(0)
            }
            
            // Check if we have enough bytes for path + payload length field
            if (offset + pathLen + LENGTH_FIELD_BYTES > buffer.size) {
                // Incomplete frame
                offset -= LENGTH_FIELD_BYTES  // rewind the path length field
                break
            }
            
            // Read path
            val path = buffer.copyOfRange(offset, offset + pathLen).toString(Charsets.UTF_8)
            offset += pathLen
            
            // Read payload length
            val payloadLen = readUint32(buffer, offset)
            offset += LENGTH_FIELD_BYTES
            
            if (payloadLen > AdbSyncConfig.MAX_PAYLOAD_SIZE || payloadLen < 0) {
                // Protocol error — discard everything and stop
                return frames to ByteArray(0)
            }
            
            // Check if we have enough bytes for payload
            if (offset + payloadLen > buffer.size) {
                // Incomplete frame
                offset -= LENGTH_FIELD_BYTES + pathLen  // rewind
                break
            }
            
            // Read payload
            val payload = buffer.copyOfRange(offset, offset + payloadLen)
            offset += payloadLen
            
            frames.add(path to payload)
        }
        
        val remaining = if (offset < buffer.size) {
            buffer.copyOfRange(offset, buffer.size)
        } else {
            ByteArray(0)
        }
        
        return frames to remaining
    }

    /**
     * Read an unsigned 32-bit big-endian integer from [buffer] at [offset].
     */
    @Suppress("UnnecessaryParentheses")
    private fun readUint32(buffer: ByteArray, offset: Int): Int {
        val b0 = buffer[offset].toInt() and 0xFF
        val b1 = buffer[offset + 1].toInt() and 0xFF
        val b2 = buffer[offset + 2].toInt() and 0xFF
        val b3 = buffer[offset + 3].toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    /**
     * Write an unsigned 32-bit big-endian integer to [buffer] at [offset].
     */
    fun writeUint32(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }
}
