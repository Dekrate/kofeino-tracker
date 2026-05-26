package pl.dekrate.kofeino.common.domain.model

import java.util.logging.Logger

/**
 * Configuration for the Wear OS caffeine tile, synced from the phone companion app.
 *
 * This model lives in [:common] so both [:app] and [:wear] modules share the
 * same data contract. No Android dependencies.
 *
 * @property displayOption Which data metric to display on the tile.
 * @property refreshIntervalMinutes Desired tile refresh interval.
 * @property colorScheme Visual color scheme for the tile.
 * @property caffeineLimitColor Whether to tint the tile red/green based on limit status.
 */
data class TileConfig(
    val displayOption: DisplayOption = DisplayOption.CAFFEINE_TOTAL,
    val refreshIntervalMinutes: RefreshInterval = RefreshInterval.MINUTES_30,
    val colorScheme: ColorScheme = ColorScheme.DEFAULT,
    val caffeineLimitColor: Boolean = true
) {
    /**
     * Returns the serialised form used for DataLayer message payload.
     * Format: "displayOption|refreshIntervalMinutes|colorScheme|caffeineLimitColor"
     */
    fun toMessagePayload(): String =
        "${displayOption.name}|${refreshIntervalMinutes.name}|${colorScheme.name}|$caffeineLimitColor"

    companion object {
        private const val REQUIRED_PART_COUNT = 4
        private const val CAFFEINE_LIMIT_FLAG_INDEX = 3

        /**
         * Deserialises a [TileConfig] from a DataLayer message payload string.
         * Returns [TileConfig] defaults if parsing fails (forward-compatible).
         */
        fun fromMessagePayload(payload: String): TileConfig {
            val parts = payload.split('|')
            if (parts.size < REQUIRED_PART_COUNT) return TileConfig()
            return try {
                TileConfig(
                    displayOption = DisplayOption.valueOf(parts[0]),
                    refreshIntervalMinutes = RefreshInterval.valueOf(parts[1]),
                    colorScheme = ColorScheme.valueOf(parts[2]),
                    caffeineLimitColor = parseBoolean(parts[CAFFEINE_LIMIT_FLAG_INDEX])
                )
            } catch (e: IllegalArgumentException) {
                Logger.getLogger(TileConfig::class.java.name).warning(
                    "Failed to parse TileConfig from message payload: ${e.message ?: "unknown"}"
                )
                TileConfig()
            }
        }
    }
}

/** Which data metric the tile should display. */
enum class DisplayOption {
    CAFFEINE_TOTAL,
    DRINK_COUNT,
    LIMIT_STATUS,
    BOTH
}

/** How often the tile should request a refresh. */
@Suppress("MagicNumber")
enum class RefreshInterval(val minutes: Int) {
    MINUTES_15(15),
    MINUTES_30(30),
    HOUR_1(60),
    HOURS_2(120)
}

/** Visual colour scheme applied to the tile. */
enum class ColorScheme {
    DEFAULT,
    MONOCHROME,
    AMBER,
    GREEN
}

/** Parses a boolean from a string, accepting "true", "1", and "yes" (case-insensitive). */
private fun parseBoolean(value: String): Boolean = when (value.trim().lowercase()) {
    "true", "1", "yes" -> true
    else -> false
}
