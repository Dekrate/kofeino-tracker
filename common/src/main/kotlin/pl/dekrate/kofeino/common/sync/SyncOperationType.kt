package pl.dekrate.kofeino.common.sync

/**
 * Operation types for sync change propagation.
 *
 * The [wireValue] is the canonical representation used on the wire.
 */
enum class SyncOperationType(val wireValue: String) {
    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");

    companion object {
        /**
         * Resolve an operation type from its [wireValue].
         * Returns `null` when the value is unknown (forward-compatibility).
         */
        fun fromWire(wireValue: String): SyncOperationType? =
            entries.find { it.wireValue.equals(wireValue, ignoreCase = true) }

        /**
         * All valid wire values for fast-path matching.
         */
        val allWireValues: Set<String> = entries.map { it.wireValue }.toSet()
    }
}
