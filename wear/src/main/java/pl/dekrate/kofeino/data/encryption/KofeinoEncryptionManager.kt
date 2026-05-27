package pl.dekrate.kofeino.data.encryption

import android.content.Context
import android.content.SharedPreferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Manages encryption keys and provides cryptographic primitives for
 * data-at-rest encryption.
 *
 * ## Architecture
 * - A single master key is stored in Android Keystore (hardware-backed on
 *   devices with StrongBox/TEE).
 * - This master key is used via Tink AEAD (AES-256-GCM) to:
 *   1. Encrypt the DataStore via [AeadSerializer] (datastore-tink)
 *   2. Encrypt the database passphrase stored in prefs
 * - The database passphrase is a random 256-bit value generated once,
 *   encrypted with the master key, and persisted in DataStore.
 *
 * ## Thread safety
 * [Aead] and [AndroidKeysetManager] are thread-safe. All suspend functions
 * are safe to call from any coroutine context.
 */
@Singleton
class KofeinoEncryptionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_URI = "android-keystore://kofeino_master_key"
        private const val KEYSET_PREFS_NAME = "kofeino_encryption_keyset"
        private const val KEYSET_PREFS_FILENAME = "kofeino_encryption_keyset_prefs"

        private const val DB_PASSPHRASE_PREFS = "kofeino_db_passphrase"
        private const val DB_PASSPHRASE_KEY = "encrypted_passphrase_hex"

        /** Length of the database passphrase in bytes (256 bits). */
        private const val PASSPHRASE_BYTES = 32
        private const val HEX_RADIX = 16
        private const val NIBBLE_WIDTH = 2
    }

    private val keysetManager: AndroidKeysetManager by lazy {
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_PREFS_NAME, KEYSET_PREFS_FILENAME)
            .withKeyTemplate(com.google.crypto.tink.KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri(KEYSTORE_URI)
            .build()
    }

    /**
     * The AEAD primitive derived from the Android Keystore master key.
     * Used by [AeadSerializer] to encrypt/decrypt DataStore.
     */
    @Suppress("Deprecation")
    val aead: Aead by lazy {
        AeadConfig.register()
        keysetManager.keysetHandle.getPrimitive(Aead::class.java)
    }

    /**
     * Returns the database encryption passphrase, generating and persisting
     * it on first call. The passphrase is a random 256-bit value, encrypted
     * with the master key and stored encrypted in a private SharedPreferences.
     *
     * @return 256-bit (32-byte) passphrase for SQLCipher.
     */
    fun getDatabasePassphrase(): ByteArray {
        val passphrasePrefs = context.getSharedPreferences(
            DB_PASSPHRASE_PREFS, Context.MODE_PRIVATE
        )

        // Try to load existing encrypted passphrase
        val encryptedHex = passphrasePrefs.getString(DB_PASSPHRASE_KEY, null)
        if (encryptedHex != null) {
            return try {
                val encrypted = hexToBytes(encryptedHex)
                aead.decrypt(encrypted, null /* associatedData */)
            } catch (e: java.security.GeneralSecurityException) {
                Timber.w(e, "Failed to decrypt database passphrase, generating new one")
                generateAndStorePassphrase(passphrasePrefs)
            }
        }

        // First run — generate new passphrase
        return generateAndStorePassphrase(passphrasePrefs)
    }

    /**
     * Check whether unencrypted legacy data exists that needs migration.
     */
    fun hasLegacyDatabase(): Boolean {
        val dbFile = context.getDatabasePath("caffeine_database")
        return dbFile.exists()
    }

    /**
     * Verify the encryption system is operational.
     * @throws IllegalStateException if encryption is not available.
     */
    fun verifyEncryptionAvailable() {
        // Force initialization of the AEAD — throws if Keystore unavailable
        val aead = aead
        val testData = "test".toByteArray()
        val encrypted = aead.encrypt(testData, null)
        val decrypted = aead.decrypt(encrypted, null)
        check(decrypted.contentEquals(testData)) {
            "Encryption verification failed: AEAD roundtrip mismatch"
        }
        Timber.i("Encryption system verified: Android Keystore + Tink AEAD available")
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun generateAndStorePassphrase(prefs: SharedPreferences): ByteArray {
        val passphrase = SecureRandom().generateSeed(PASSPHRASE_BYTES)
        val encrypted = aead.encrypt(passphrase, null /* associatedData */)
        prefs.edit()
            .putString(DB_PASSPHRASE_KEY, bytesToHex(encrypted))
            .apply()
        Timber.i("Generated and stored new encrypted database passphrase")
        return passphrase
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString(separator = "") { byte ->
            java.lang.String.format(java.util.Locale.ROOT, "%02x", byte)
        }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        require(len % 2 == 0) { "Invalid hex string length: $len" }
        return ByteArray(len / NIBBLE_WIDTH) {
            hex.substring(it * NIBBLE_WIDTH, it * NIBBLE_WIDTH + NIBBLE_WIDTH).toInt(HEX_RADIX).toByte()
        }
    }
}
