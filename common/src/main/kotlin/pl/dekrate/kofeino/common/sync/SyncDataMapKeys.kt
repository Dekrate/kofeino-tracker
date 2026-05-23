package pl.dekrate.kofeino.common.sync

/**
 * Canonical DataMap key constants for the sync protocol.
 *
 * These **must** be used by both devices when building or parsing DataMaps
 * so that schema drift between :app and :wear is impossible.
 *
 * ## Forward compatibility
 * Receivers **must** ignore unknown keys. Adding new keys in a future
 * version must never crash an older peer.
 */
object SyncDataMapKeys {

    // ─── Common fields (present in every synced entity) ──────────────
    const val KEY_ID = "id"
    const val KEY_LAST_MODIFIED_TIMESTAMP = "lastModifiedTimestamp"
    const val KEY_SOURCE_DEVICE_ID = "sourceDeviceId"

    // ─── CaffeineIntake fields ───────────────────────────────────────
    object Intake {
        const val DRINK_ID = "drinkId"
        const val DRINK_NAME = "drinkName"
        const val CAFFEINE_MG = "caffeineMg"
        const val VOLUME_ML = "volumeMl"
        const val TIMESTAMP = "timestamp"
    }

    // ─── DrinkEntity fields ──────────────────────────────────────────
    object Drink {
        const val NAME = "name"
        const val CAFFEINE_MG = "caffeineMg"
        const val VOLUME_ML = "volumeMl"
        const val IS_DEFAULT = "isDefault"
        const val IS_OFFICIAL = "isOfficial"
    }

    // ─── Settings fields ─────────────────────────────────────────────
    object Settings {
        const val CAFFEINE_LIMIT_PROFILE = "caffeineLimitProfile"
        const val CUSTOM_LIMIT_MG = "customLimitMg"
        const val LANGUAGE = "language"
        const val THEME = "theme"
        const val NOTIFICATION_ENABLED = "notificationEnabled"
    }

    // ─── Sync‑metadata fields ────────────────────────────────────────
    object Sync {
        /** Full‑sync operation: SHA‑256 state hash of all local entities. */
        const val STATE_HASH = "stateHash"

        /** Timestamp of the last successful sync (epoch millis). */
        const val LAST_SYNC_TIMESTAMP = "lastSyncTimestamp"

        /** Entity type carried in a full‑sync response batch. */
        const val ENTITY_TYPE = "entityType"

        /** Operation type for an incremental change. */
        const val OPERATION_TYPE = "operationType"

        /** JSON‑encoded list of entities in a full‑sync batch. */
        const val ENTITIES_JSON = "entitiesJson"
    }
}
