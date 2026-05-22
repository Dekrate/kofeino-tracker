package pl.dekrate.kofeino.tracker.data.sync

import com.google.gson.Gson
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

/**
 * JSON serializer/deserializer for cross-device sync payloads.
 *
 * Uses Gson (already a project dependency) for all conversions.
 *
 * ## Design notes
 * - **Single responsibility**: owns the serialization format for sync messages.
 * - **Centralized**: one place to update when domain models change.
 * - **Fail-fast**: deserialization throws on malformed payloads; callers
 *   should catch and handle gracefully.
 */
object SyncPayloadSerializer {

    private val gson = Gson()

    // ------------------------------------------------------------------
    // Serialize (domain → JSON string)
    // ------------------------------------------------------------------

    /** Serialize a [CaffeineIntake] to its JSON sync payload. */
    fun serializeIntake(intake: CaffeineIntake): String =
        gson.toJson(intake)

    /** Serialize a [DrinkEntity] to its JSON sync payload. */
    fun serializeDrink(drink: DrinkEntity): String =
        gson.toJson(drink)

    // ------------------------------------------------------------------
    // Deserialize (JSON string → domain)
    // ------------------------------------------------------------------

    /** Deserialize a JSON sync payload into a [CaffeineIntake]. */
    fun deserializeIntake(json: String): CaffeineIntake =
        gson.fromJson(json, CaffeineIntake::class.java)

    /** Deserialize a JSON sync payload into a [DrinkEntity]. */
    fun deserializeDrink(json: String): DrinkEntity =
        gson.fromJson(json, DrinkEntity::class.java)
}
