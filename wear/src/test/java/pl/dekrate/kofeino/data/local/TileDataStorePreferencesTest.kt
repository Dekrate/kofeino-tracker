package pl.dekrate.kofeino.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pl.dekrate.kofeino.common.domain.model.ColorScheme
import pl.dekrate.kofeino.common.domain.model.DisplayOption
import pl.dekrate.kofeino.common.domain.model.RefreshInterval
import pl.dekrate.kofeino.common.domain.model.TileConfig

@RunWith(RobolectricTestRunner::class)
class TileDataStorePreferencesTest {

    private lateinit var context: Context
    private lateinit var tilePreferences: TileDataStorePreferences
    private lateinit var tileConfigStore: DataStore<Preferences>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tileConfigStore = DataStoreFactory.create(
            serializer = PlainPreferencesSerializer(),
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(context.cacheDir, "test_tile_config.preferences_pb") }
        )
        tilePreferences = TileDataStorePreferences(tileConfigStore)
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

/**
 * A non-encrypted [Serializer] that bridges the Okio-based
 * [PreferencesSerializer] to the Java IO-based [Serializer] interface,
 * used for testing.
 */
private class PlainPreferencesSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences = emptyPreferences()

    override suspend fun readFrom(input: InputStream): Preferences {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return emptyPreferences()
        val source: BufferedSource = Buffer().apply { write(bytes) }
        return PreferencesSerializer.readFrom(source)
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val sink: BufferedSink = Buffer()
        PreferencesSerializer.writeTo(t, sink)
        val bytes = (sink as Buffer).readByteArray()
        output.write(bytes)
    }
}
