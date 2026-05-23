package pl.dekrate.kofeino.common.sync

/**
 * Entity types that can be synchronised across devices.
 *
 * The [pathSegment] value is the canonical wire representation used in
 * path components and payload metadata.
 */
enum class SyncEntityType(val pathSegment: String) {
    INTAKE("intake"),
    DRINK("drink"),
    SETTINGS("settings");

    companion object {
        /**
         * Resolve an entity type from its wire [pathSegment].
         * Returns `null` when the segment is unknown (forward-compatibility).
         */
        fun fromPath(pathSegment: String): SyncEntityType? =
            entries.find { it.pathSegment.equals(pathSegment, ignoreCase = true) }

        /**
         * All valid path segments for fast-path matching.
         */
        val allPathSegments: Set<String> = entries.map { it.pathSegment }.toSet()
    }
}
