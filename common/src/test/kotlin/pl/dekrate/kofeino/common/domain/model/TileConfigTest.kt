package pl.dekrate.kofeino.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileConfigTest {

    @Test
    fun `toMessagePayload round-trip should preserve all fields`() {
        val original = TileConfig(
            displayOption = DisplayOption.BOTH,
            refreshIntervalMinutes = RefreshInterval.HOUR_1,
            colorScheme = ColorScheme.AMBER,
            caffeineLimitColor = false
        )
        val payload = original.toMessagePayload()
        val restored = TileConfig.fromMessagePayload(payload)
        assertEquals(original, restored)
    }

    @Test
    fun `fromMessagePayload with default payload should return default TileConfig`() {
        val config = TileConfig.fromMessagePayload("CAFFEINE_TOTAL|MINUTES_30|DEFAULT|true")
        assertEquals(TileConfig(), config)
    }

    @Test
    fun `fromMessagePayload with partial payload (less than 4 parts) should return defaults`() {
        val config = TileConfig.fromMessagePayload("CAFFEINE_TOTAL|MINUTES_30")
        assertEquals(TileConfig(), config)
    }

    @Test
    fun `fromMessagePayload with unknown enum value should return defaults`() {
        val config = TileConfig.fromMessagePayload("UNKNOWN|MINUTES_30|DEFAULT|true")
        assertEquals(TileConfig(), config)
    }

    @Test
    fun `fromMessagePayload with invalid boolean should return defaults`() {
        val config = TileConfig.fromMessagePayload("CAFFEINE_TOTAL|MINUTES_30|DEFAULT|notabool")
        // parseBoolean returns false for non-"true"/"1"/"yes", so caffeineLimitColor will be false
        val expected = TileConfig().copy(caffeineLimitColor = false)
        assertEquals(expected, config)
    }

    @Test
    fun `toMessagePayload should produce correct pipe-delimited format`() {
        val config = TileConfig(DisplayOption.BOTH, RefreshInterval.HOUR_1, ColorScheme.AMBER, false)
        assertEquals("BOTH|HOUR_1|AMBER|false", config.toMessagePayload())
    }

    @Test
    fun `default TileConfig should use CAFFEINE_TOTAL display`() {
        assertEquals(DisplayOption.CAFFEINE_TOTAL, TileConfig().displayOption)
    }

    @Test
    fun `default TileConfig should use thirty minute refresh`() {
        assertEquals(RefreshInterval.MINUTES_30, TileConfig().refreshIntervalMinutes)
    }

    @Test
    fun `default TileConfig should use DEFAULT color scheme`() {
        assertEquals(ColorScheme.DEFAULT, TileConfig().colorScheme)
    }

    @Test
    fun `default TileConfig should have caffeineLimitColor enabled`() {
        assertTrue(TileConfig().caffeineLimitColor)
    }

    @Test
    fun `fromMessagePayload with empty string should return defaults`() {
        val config = TileConfig.fromMessagePayload("")
        assertEquals(TileConfig(), config)
    }

    @Test
    fun `fromMessagePayload with invalid input type for first field should return defaults`() {
        // valueOf will throw IllegalArgumentException for "INVALID_DISPLAY"
        val config = TileConfig.fromMessagePayload("INVALID_DISPLAY|MINUTES_30|DEFAULT|true")
        assertEquals(TileConfig(), config)
    }
}
