package pl.dekrate.kofeino.data.sync

import com.google.gson.Gson
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

/**
 * JSON serializer/deserializer for cross-device sync payloads (wear module).
 *
 * Uses Gson (already a project dependency) for all conversions.
 *
 * ## Design notes
 * - Mirrors [pl.dekrate.kofeino.tracker.data.sync.SyncPayloadSerializer] in the app module.
 * - Uses the wear module's own domain model types.
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
