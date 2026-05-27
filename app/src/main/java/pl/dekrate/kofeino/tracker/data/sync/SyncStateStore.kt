package pl.dekrate.kofeino.tracker.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pl.dekrate.kofeino.tracker.di.SyncStateDataStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed persistent store for cross-device sync metadata.
 *
 * ## Stored values
 * - **lastSyncTimestamp**: Epoch millis of the last successful full-sync completion.
 *   A value of `0L` means no sync has ever completed (first-time connection).
 * - **lastStateHash**: Hex-encoded SHA-256 combined hash of all entities at the
 *   moment of last successful sync. Used for quick divergence detection.
 * - **lastSyncedDeviceId**: The Wear node ID of the device we last completed a
 *   full sync with. Allows detecting reconnection with the same device.
 *
 * ## Thread safety
 * DataStore guarantees thread safety. All reads/writes are sequential.
 *
 * ## Error handling
 * Corrupt data returns defaults (0L / empty string) — the sync protocol
 * will detect divergence and perform a full sync as a recovery mechanism.
 */
@Singleton
class SyncStateStore @Inject constructor(
    @SyncStateDataStore private val syncStateStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        private val KEY_LAST_STATE_HASH = stringPreferencesKey("last_state_hash")
        private val KEY_LAST_SYNCED_DEVICE_ID = stringPreferencesKey("last_synced_device_id")

        const val DEFAULT_LAST_SYNC_TIMESTAMP = 0L
        const val DEFAULT_LAST_STATE_HASH = ""
        const val DEFAULT_LAST_SYNCED_DEVICE_ID = ""

        /** Number of characters to log for the state hash prefix. */
        private const val LOG_HASH_PREFIX_LENGTH = 16
    }

    // ------------------------------------------------------------------
    // Observable flows (for UI / CrossDeviceStatusScreen)
    // ------------------------------------------------------------------

    /** Observe the last sync timestamp as a [Flow]. */
    val lastSyncTimestampFlow: Flow<Long> = syncStateStore.data.map { preferences ->
        preferences[KEY_LAST_SYNC_TIMESTAMP] ?: DEFAULT_LAST_SYNC_TIMESTAMP
    }

    /** Observe the last known combined state hash. */
    val lastStateHashFlow: Flow<String> = syncStateStore.data.map { preferences ->
        preferences[KEY_LAST_STATE_HASH] ?: DEFAULT_LAST_STATE_HASH
    }

    /** Observe the last synced device ID. */
    val lastSyncedDeviceIdFlow: Flow<String> = syncStateStore.data.map { preferences ->
        preferences[KEY_LAST_SYNCED_DEVICE_ID] ?: DEFAULT_LAST_SYNCED_DEVICE_ID
    }

    // ------------------------------------------------------------------
    // One-shot reads
    // ------------------------------------------------------------------

    /** Get the last sync timestamp. `0L` means never synced. */
    suspend fun getLastSyncTimestamp(): Long =
        syncStateStore.data.first()[KEY_LAST_SYNC_TIMESTAMP] ?: DEFAULT_LAST_SYNC_TIMESTAMP

    /** Get the last known combined state hash. */
    suspend fun getLastStateHash(): String =
        syncStateStore.data.first()[KEY_LAST_STATE_HASH] ?: DEFAULT_LAST_STATE_HASH

    /** Get the last synced device ID. */
    suspend fun getLastSyncedDeviceId(): String =
        syncStateStore.data.first()[KEY_LAST_SYNCED_DEVICE_ID] ?: DEFAULT_LAST_SYNCED_DEVICE_ID

    // ------------------------------------------------------------------
    // Mutation
    // ------------------------------------------------------------------

    /**
     * Update all sync state values atomically after a successful full sync.
     *
     * @param timestamp Epoch millis of sync completion.
     * @param stateHash Combined SHA-256 hash of all entities at sync time.
     * @param deviceId The node ID of the device we synced with.
     */
    suspend fun recordSyncCompletion(
        timestamp: Long,
        stateHash: String,
        deviceId: String
    ) {
        syncStateStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
            preferences[KEY_LAST_STATE_HASH] = stateHash
            preferences[KEY_LAST_SYNCED_DEVICE_ID] = deviceId
        }
        Timber.d("SyncStateStore: recorded sync completion ts=%d hash=%s device=%s",
            timestamp, stateHash.take(LOG_HASH_PREFIX_LENGTH), deviceId)
    }

    /**
     * Reset all sync state to defaults.
     *
     * Useful after a full resync or when the user explicitly requests it.
     */
    suspend fun reset() {
        syncStateStore.edit { preferences ->
            preferences.clear()
        }
        Timber.d("SyncStateStore: reset to defaults")
    }
}
