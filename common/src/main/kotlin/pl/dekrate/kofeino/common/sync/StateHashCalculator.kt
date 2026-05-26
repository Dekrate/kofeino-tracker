package pl.dekrate.kofeino.common.sync

import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import java.security.MessageDigest

/**
 * Computes deterministic SHA-256 state hashes for local entity collections.
 *
 * Used by the full-sync protocol to detect divergence between paired devices
 * without transferring all data: if the hashes match, no sync is needed.
 *
 * ## Determinism guarantees
 * - Entities are **sorted by stable key** (ID) before hashing.
 * - Each entity is serialised via its canonical [toString] representation.
 * - The combined hash is computed from concatenated sorted entity hashes.
 *
 * ## Thread safety
 * This object is stateless and thread-safe. [MessageDigest] instances are
 * created per-call and never shared.
 */
object StateHashCalculator {

    /** Hex-encoded SHA-256 length (64 chars). */
    private const val HASH_LENGTH = 64

    /** Separator used between entity hashes in combined hash computation. */
    private const val HASH_SEPARATOR = "|"

    /**
     * Compute a SHA-256 hash for the given [intakes].
     *
     * The hash is deterministic for the same set of entities with the same
     * field values, regardless of insertion order.
     */
    fun computeIntakeHash(intakes: List<CaffeineIntake>): String {
        val sorted = intakes.sortedBy { it.id }
        val digester = MessageDigest.getInstance("SHA-256")
        for (intake in sorted) {
            digester.update(intake.toHashString().toByteArray(Charsets.UTF_8))
            digester.update(0.toByte()) // null separator between entities
        }
        return digester.digest().toHexString()
    }

    /**
     * Compute a SHA-256 hash for the given [drinks].
     *
     * The hash is deterministic for the same set of entities with the same
     * field values, regardless of insertion order.
     */
    fun computeDrinkHash(drinks: List<DrinkEntity>): String {
        val sorted = drinks.sortedBy { it.id }
        val digester = MessageDigest.getInstance("SHA-256")
        for (drink in sorted) {
            digester.update(drink.toHashString().toByteArray(Charsets.UTF_8))
            digester.update(0.toByte()) // null separator between entities
        }
        return digester.digest().toHexString()
    }

    /**
     * Compute a single combined SHA-256 hash from both [intakes] and [drinks].
     *
     * This is the hash used for full-sync comparison: if the combined hash
     * on both devices matches, no sync is needed.
     */
    fun computeCombinedHash(
        intakes: List<CaffeineIntake>,
        drinks: List<DrinkEntity>
    ): String {
        val intakeHash = computeIntakeHash(intakes)
        val drinkHash = computeDrinkHash(drinks)
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(intakeHash.toByteArray(Charsets.UTF_8))
        digester.update(HASH_SEPARATOR.toByteArray(Charsets.UTF_8))
        digester.update(drinkHash.toByteArray(Charsets.UTF_8))
        return digester.digest().toHexString()
    }

    /**
     * Verify whether a previously-computed [hash] matches the current
     * state of [intakes] and [drinks].
     *
     * @return `true` if the hash matches, `false` otherwise.
     */
    fun verifyHash(
        hash: String,
        intakes: List<CaffeineIntake>,
        drinks: List<DrinkEntity>
    ): Boolean {
        if (hash.length != HASH_LENGTH) return false
        return hash == computeCombinedHash(intakes, drinks)
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Canonical string representation of a [CaffeineIntake] for hashing.
     *
     * Includes all fields that affect sync state. Order is stable.
     */
    private fun CaffeineIntake.toHashString(): String =
        buildString {
            append("intake:")
            append(id)
            append(",drinkId:")
            append(drinkId)
            append(",caffeineMg:")
            append(caffeineMg)
            append(",volumeMl:")
            append(volumeMl)
            append(",timestamp:")
            append(timestamp)
            append(",lastModifiedTimestamp:")
            append(lastModifiedTimestamp)
            append(",sourceDeviceId:")
            append(sourceDeviceId)
        }

    /**
     * Canonical string representation of a [DrinkEntity] for hashing.
     *
     * Includes all fields that affect sync state. Order is stable.
     */
    private fun DrinkEntity.toHashString(): String =
        buildString {
            append("drink:")
            append(id)
            append(",name:")
            append(name)
            append(",caffeineMg:")
            append(caffeineMg)
            append(",volumeMl:")
            append(volumeMl)
            append(",isDefault:")
            append(isDefault)
            append(",lastModifiedTimestamp:")
            append(lastModifiedTimestamp)
            append(",sourceDeviceId:")
            append(sourceDeviceId)
        }

    /**
     * Convert a byte array to a lowercase hex string.
     */
    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }
}
