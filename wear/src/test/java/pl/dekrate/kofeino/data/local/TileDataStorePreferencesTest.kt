package pl.dekrate.kofeino.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.first
import pl.dekrate.kofeino.common.domain.model.ColorScheme
import pl.dekrate.kofeino.common.domain.model.DisplayOption
import pl.dekrate.kofeino.common.domain.model.RefreshInterval
import pl.dekrate.kofeino.common.domain.model.TileConfig

@RunWith(RobolectricTestRunner::class)
class TileDataStorePreferencesTest {

    private lateinit var context: Context
    private lateinit var tilePreferences: TileDataStorePreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tilePreferences = TileDataStorePreferences(context)
        // Ensure a clean state before each test
        runTest { tilePreferences.reset() }
    }

    @After
    fun tearDown() {
        runTest { tilePreferences.reset() }
    }

    @Test
    fun `getTileConfig returns defaults when no data stored`() = runTest {
        val config = tilePreferences.getTileConfig()
        assertEquals(TileConfig(), config)
    }

    @Test
    fun `setTileConfig and getTileConfig round-trip preserves all fields`() = runTest {
        val config = TileConfig(
            displayOption = DisplayOption.BOTH,
            refreshIntervalMinutes = RefreshInterval.HOUR_1,
            colorScheme = ColorScheme.AMBER,
            caffeineLimitColor = false
        )
        tilePreferences.setTileConfig(config)
        val retrieved = tilePreferences.getTileConfig()
        assertEquals(config, retrieved)
    }

    @Test
    fun `tileConfigFlow emits initial default value`() = runTest {
        val flowValue = tilePreferences.tileConfigFlow.first()
        assertEquals(TileConfig(), flowValue)
    }

    @Test
    fun `tileConfigFlow emits updated value after setTileConfig`() = runTest {
        val config = TileConfig(displayOption = DisplayOption.LIMIT_STATUS)
        tilePreferences.setTileConfig(config)
        val flowValue = tilePreferences.tileConfigFlow.first()
        assertEquals(DisplayOption.LIMIT_STATUS, flowValue.displayOption)
    }
}
