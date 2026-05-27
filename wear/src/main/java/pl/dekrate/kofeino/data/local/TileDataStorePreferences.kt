package pl.dekrate.kofeino.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pl.dekrate.kofeino.common.domain.model.TileConfig
import pl.dekrate.kofeino.di.TileConfigDataStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed persistent store for the Wear OS Tile configuration.
 *
 * Stores serialised [TileConfig] that was synced from the phone companion app.
 * The watch uses default [TileConfig] values when no phone config has been received
 * yet (standalone-first design).
 *
 * ## Thread safety
 * DataStore guarantees thread safety. All reads/writes are sequential.
 *
 * ## Error handling
 * Corrupt data returns [TileConfig] defaults — the tile degrades gracefully
 * to its default display mode.
 */
@Singleton
class TileDataStorePreferences @Inject constructor(
    @TileConfigDataStore private val tileConfigStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_TILE_CONFIG = stringPreferencesKey("tile_config_payload")
    }

    // ------------------------------------------------------------------
    // Observable flows
    // ------------------------------------------------------------------

    /** Observe the current tile configuration as a [Flow]. */
    val tileConfigFlow: Flow<TileConfig> = tileConfigStore.data.map { preferences ->
        val payload = preferences[KEY_TILE_CONFIG]
        if (payload != null) {
            TileConfig.fromMessagePayload(payload)
        } else {
            TileConfig()
        }
    }

    // ------------------------------------------------------------------
    // One-shot reads
    // ------------------------------------------------------------------

    /** Get the current tile configuration (falls back to defaults). */
    suspend fun getTileConfig(): TileConfig {
        val payload = tileConfigStore.data.first()[KEY_TILE_CONFIG]
        return if (payload != null) {
            TileConfig.fromMessagePayload(payload)
        } else {
            TileConfig()
        }
    }

    // ------------------------------------------------------------------
    // Mutation
    // ------------------------------------------------------------------

    /**
     * Persist a new tile configuration received from the phone.
     *
     * @param config The [TileConfig] to store.
     */
    suspend fun setTileConfig(config: TileConfig) {
        tileConfigStore.edit { preferences ->
            preferences[KEY_TILE_CONFIG] = config.toMessagePayload()
        }
        Timber.d("TileDataStorePreferences: tile config updated — display=%s refresh=%dmin",
            config.displayOption.name, config.refreshIntervalMinutes.minutes)
    }

    /**
     * Reset tile configuration to defaults (clears stored value).
     */
    suspend fun reset() {
        tileConfigStore.edit { preferences ->
            preferences.clear()
        }
        Timber.d("TileDataStorePreferences: reset to defaults")
    }
}
