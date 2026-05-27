package pl.dekrate.kofeino.tracker.data.encryption

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import com.google.crypto.tink.Aead
import java.io.InputStream
import java.io.OutputStream
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * A [Serializer] that encrypts/decrypts preferences data using Tink AEAD.
 *
 * Wraps [PreferencesSerializer] for the actual serialization but encrypts
 * the raw bytes before persisting. This provides encryption at the
 * serializer level without depending on the older [AeadSerializer]
 * which is incompatible with the Okio-based [PreferencesSerializer] in
 * DataStore 1.3.x.
 */
class EncryptedPreferencesSerializer(
    private val aead: Aead,
    private val associatedData: ByteArray = ByteArray(0)
) : Serializer<Preferences> {

    override val defaultValue: Preferences = emptyPreferences()

    override suspend fun readFrom(input: InputStream): Preferences {
        val encryptedBytes = input.readBytes()
        if (encryptedBytes.isEmpty()) return emptyPreferences()
        val decryptedBytes = try {
            aead.decrypt(encryptedBytes, associatedData)
        } catch (e: java.io.IOException) {
            throw CorruptionException("Failed to decrypt preferences data", e)
        }
        val source: BufferedSource = Buffer().apply { write(decryptedBytes) }
        return PreferencesSerializer.readFrom(source)
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val sink: BufferedSink = Buffer()
        PreferencesSerializer.writeTo(t, sink)
        val serializedBytes = (sink as Buffer).readByteArray()
        val encryptedBytes = aead.encrypt(serializedBytes, associatedData)
        output.write(encryptedBytes)
    }
}
