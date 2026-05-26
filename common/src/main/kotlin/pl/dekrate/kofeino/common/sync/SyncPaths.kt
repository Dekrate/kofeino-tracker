package pl.dekrate.kofeino.common.sync

/**
 * Centralised path and message-path constants for the cross-device sync protocol.
 *
 * These constants **must** be used by both `:app` and `:wear` modules so that
 * sender and receiver agree on the same routing -- no magic strings.
 *
 * ## Path conventions
 * - `/sync/…`      — DataItem / bulk-sync paths (future use with `PutDataMapRequest`)
 * - `/message/…`   — Real‑time message paths used with `MessageClient.sendMessage`
 *
 * ## Forward compatibility
 * Consumers **must** ignore paths they do not recognise so that adding new
 * entity types in a future version never crashes an older peer.
 */
object SyncPaths {

    // ─── Prefixes ────────────────────────────────────────────────────
    const val SYNC_PATH_PREFIX = "/sync"
    const val MESSAGE_PATH_PREFIX = "/message"

    // ─── Data‑sync paths (intended for PutDataMapRequest / DataItems) ─
    const val INTAKES_PATH = "$SYNC_PATH_PREFIX/intakes"
    const val DRINKS_PATH = "$SYNC_PATH_PREFIX/drinks"
    const val SETTINGS_PATH = "$SYNC_PATH_PREFIX/settings"

    // ─── Real‑time message paths (MessageClient.sendMessage) ─────────
    const val MESSAGE_INTAKE_ADDED = "$MESSAGE_PATH_PREFIX/intake-added"
    const val MESSAGE_INTAKE_DELETED = "$MESSAGE_PATH_PREFIX/intake-deleted"
    const val MESSAGE_INTAKE_UPDATED = "$MESSAGE_PATH_PREFIX/intake-updated"
    const val MESSAGE_DRINK_CREATED = "$MESSAGE_PATH_PREFIX/drink-created"
    const val MESSAGE_DRINK_UPDATED = "$MESSAGE_PATH_PREFIX/drink-updated"
    const val MESSAGE_DRINK_DELETED = "$MESSAGE_PATH_PREFIX/drink-deleted"
    const val MESSAGE_SETTINGS_CHANGED = "$MESSAGE_PATH_PREFIX/settings-changed"

    // ─── Full‑sync message paths ─────────────────────────────────────
    const val MESSAGE_FULL_SYNC_REQUEST = "$MESSAGE_PATH_PREFIX/full-sync-request"
    const val MESSAGE_FULL_SYNC_RESPONSE = "$MESSAGE_PATH_PREFIX/full-sync-response"
    const val MESSAGE_SYNC_ACK = "$MESSAGE_PATH_PREFIX/sync-ack"

    // ─── Tile configuration ───────────────────────────────────────────
    const val MESSAGE_TILE_CONFIG_CHANGED = "$MESSAGE_PATH_PREFIX/tile-config-changed"

    // ─── Builder helpers ─────────────────────────────────────────────

    /** Build a sync path for the given [entityType], e.g. `/sync/intakes`. */
    fun entityPath(entityType: SyncEntityType): String =
        "$SYNC_PATH_PREFIX/${entityType.pathSegment}"

    /** Build a real‑time message path for the given segments. */
    fun messagePath(vararg segments: String): String =
        segments.joinToString(separator = "/", prefix = "$MESSAGE_PATH_PREFIX/")

    /** Build a sync path for the given segments. */
    fun syncPath(vararg segments: String): String =
        segments.joinToString(separator = "/", prefix = "$SYNC_PATH_PREFIX/")
}
