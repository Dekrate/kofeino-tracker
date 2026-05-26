package pl.dekrate.kofeino.tracker.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.backup.BackupIOException
import pl.dekrate.kofeino.tracker.data.backup.BackupManager
import pl.dekrate.kofeino.tracker.data.backup.BackupVersionException
import pl.dekrate.kofeino.tracker.data.local.CaffeineLimitProfile
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.di.IoDispatcher
import pl.dekrate.kofeino.common.domain.model.TileConfig
import pl.dekrate.kofeino.common.domain.model.DisplayOption
import pl.dekrate.kofeino.common.domain.model.RefreshInterval
import pl.dekrate.kofeino.common.domain.model.ColorScheme
import pl.dekrate.kofeino.tracker.data.sync.WearableDataLayerManager
import javax.inject.Inject

data class SettingsUiState(
    val currentLanguage: String = DataStorePreferences.DEFAULT_LANGUAGE,
    val languageChanged: Boolean = false,
    val currentThemeMode: String = DataStorePreferences.DEFAULT_THEME,
    val themeChanged: Boolean = false,
    // Notification toggles
    val notifLiveEnabled: Boolean = DataStorePreferences.DEFAULT_NOTIF_LIVE,
    val notifMorningEnabled: Boolean = DataStorePreferences.DEFAULT_NOTIF_MORNING,
    val notifRegularEnabled: Boolean = DataStorePreferences.DEFAULT_NOTIF_REGULAR,
    val notifEveningEnabled: Boolean = DataStorePreferences.DEFAULT_NOTIF_EVENING,
    // Caffeine limit profile
    val currentCaffeineProfile: CaffeineLimitProfile = CaffeineLimitProfile.ADULT,
    val currentCustomLimit: Int = DataStorePreferences.DEFAULT_CUSTOM_CAFFEINE_LIMIT,
    // Backup / Restore state
    val backupState: BackupUiState = BackupUiState.Idle,
    /** Whether to import settings from the backup file (toggled by user in UI). */
    val importSettingsEnabled: Boolean = false,
    /** Configuration for the Wear OS caffeine tile. */
    val tileConfig: TileConfig = TileConfig()
)

/** Sealed representation of the backup/restore UI state for button gating. */
sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Exporting : BackupUiState
    data object Importing : BackupUiState
    data object Success : BackupUiState
    data object Error : BackupUiState
}

/**
 * One-shot navigation / snackbar events for the Settings screen.
 * Messages are pre-resolved in the ViewModel via [context.getString] so the
 * UI layer does not need to resolve resource IDs inside a LaunchedEffect.
 */
sealed interface SettingsEvent {
    data class ShowSnackbar(val message: String) : SettingsEvent
}

/**
 * ViewModel for the Settings screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: DataStorePreferences,
    private val backupManager: BackupManager,
    private val wearableDataLayerManager: WearableDataLayerManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val lang = preferences.observeLanguage().first()
            val theme = preferences.observeThemeMode().first()
            val notifLive = preferences.observeNotificationLiveEnabled().first()
            val notifMorning = preferences.observeNotificationMorningEnabled().first()
            val notifRegular = preferences.observeNotificationRegularEnabled().first()
            val notifEvening = preferences.observeNotificationEveningEnabled().first()
            val profile = preferences.observeCaffeineProfile().first()
            val customLimit = preferences.observeCustomCaffeineLimit().first()
            val tileConfig = TileConfig()  // Future: load persisted TileConfig from DataStore
            _uiState.update {
                it.copy(
                    currentLanguage = lang, currentThemeMode = theme,
                    notifLiveEnabled = notifLive,
                    notifMorningEnabled = notifMorning,
                    notifRegularEnabled = notifRegular,
                    notifEveningEnabled = notifEvening,
                    currentCaffeineProfile = profile,
                    currentCustomLimit = customLimit,
                    tileConfig = tileConfig
                )
            }
        }
    }

    fun setLanguage(lang: String) {
        if (lang == _uiState.value.currentLanguage) return
        viewModelScope.launch {
            preferences.setLanguage(lang)
            _uiState.update { it.copy(currentLanguage = lang, languageChanged = true) }
        }
    }

    fun consumeLanguageChanged() {
        _uiState.update { it.copy(languageChanged = false) }
    }

    fun setThemeMode(mode: String) {
        if (mode == _uiState.value.currentThemeMode) return
        viewModelScope.launch {
            preferences.setThemeMode(mode)
            _uiState.update { it.copy(currentThemeMode = mode, themeChanged = true) }
        }
    }

    fun consumeThemeChanged() {
        _uiState.update { it.copy(themeChanged = false) }
    }

    // ===== Notification toggles =====

    fun setNotifLiveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationLiveEnabled(enabled)
            _uiState.update { it.copy(notifLiveEnabled = enabled) }
        }
    }

    fun setNotifMorningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationMorningEnabled(enabled)
            _uiState.update { it.copy(notifMorningEnabled = enabled) }
        }
    }

    fun setNotifRegularEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationRegularEnabled(enabled)
            _uiState.update { it.copy(notifRegularEnabled = enabled) }
        }
    }

    fun setNotifEveningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationEveningEnabled(enabled)
            _uiState.update { it.copy(notifEveningEnabled = enabled) }
        }
    }

    // ===== Caffeine Limit Profile =====

    fun setCaffeineProfile(profile: CaffeineLimitProfile) {
        if (profile == _uiState.value.currentCaffeineProfile) return
        viewModelScope.launch {
            preferences.setCaffeineProfile(profile)
            _uiState.update { it.copy(currentCaffeineProfile = profile) }
        }
    }

    fun setCustomCaffeineLimit(mg: Int) {
        if (mg == _uiState.value.currentCustomLimit) return
        viewModelScope.launch {
            preferences.setCustomCaffeineLimit(mg)
            _uiState.update { it.copy(currentCustomLimit = mg) }
        }
    }

    // ===== Backup / Restore =====

    /** Toggle whether to import settings from the backup file. */
    fun setImportSettingsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(importSettingsEnabled = enabled) }
    }

    /** Export all data to the SAF-chosen [uri]. */
    fun exportBackup(uri: Uri) {
        if (_uiState.value.backupState is BackupUiState.Exporting) return
        _uiState.update { it.copy(backupState = BackupUiState.Exporting) }

        viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    backupManager.exportBackup(uri)
                }
                _uiState.update { it.copy(backupState = BackupUiState.Success) }
                _events.emit(SettingsEvent.ShowSnackbar(
                    context.getString(R.string.backup_export_success, result.intakeCount, result.drinkCount)
                ))
            } catch (e: BackupIOException) {
                _uiState.update { it.copy(backupState = BackupUiState.Error) }
                _events.emit(SettingsEvent.ShowSnackbar(
                    context.getString(R.string.backup_error, e.message.orEmpty())
                ))
            }
        }
    }

    /** Import data from the SAF-chosen [uri]. */
    fun importBackup(uri: Uri) {
        if (_uiState.value.backupState is BackupUiState.Importing) return
        val importSettings = _uiState.value.importSettingsEnabled
        _uiState.update { it.copy(backupState = BackupUiState.Importing) }

        viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    backupManager.importBackup(uri, importSettings)
                }
                _uiState.update { it.copy(backupState = BackupUiState.Success) }
                _events.emit(SettingsEvent.ShowSnackbar(
                    context.getString(R.string.backup_import_success, result.intakesImported, result.drinksImported)
                ))
            } catch (e: BackupVersionException) {
                _uiState.update { it.copy(backupState = BackupUiState.Error) }
                _events.emit(SettingsEvent.ShowSnackbar(
                    context.getString(R.string.backup_error, e.message.orEmpty())
                ))
            } catch (e: BackupIOException) {
                _uiState.update { it.copy(backupState = BackupUiState.Error) }
                _events.emit(SettingsEvent.ShowSnackbar(
                    context.getString(R.string.backup_error, e.message.orEmpty())
                ))
            }
        }
    }

    // ===== Wear OS Tile Configuration =====

    fun setTileDisplayOption(option: DisplayOption) {
        if (option == _uiState.value.tileConfig.displayOption) return
        viewModelScope.launch {
            val newConfig = _uiState.value.tileConfig.copy(displayOption = option)
            _uiState.update { it.copy(tileConfig = newConfig) }
            sendTileConfigToWatch(newConfig)
        }
    }

    fun setTileRefreshInterval(interval: RefreshInterval) {
        if (interval == _uiState.value.tileConfig.refreshIntervalMinutes) return
        viewModelScope.launch {
            val newConfig = _uiState.value.tileConfig.copy(refreshIntervalMinutes = interval)
            _uiState.update { it.copy(tileConfig = newConfig) }
            sendTileConfigToWatch(newConfig)
        }
    }

    fun setTileColorScheme(scheme: ColorScheme) {
        if (scheme == _uiState.value.tileConfig.colorScheme) return
        viewModelScope.launch {
            val newConfig = _uiState.value.tileConfig.copy(colorScheme = scheme)
            _uiState.update { it.copy(tileConfig = newConfig) }
            sendTileConfigToWatch(newConfig)
        }
    }

    fun setTileCaffeineLimitColor(enabled: Boolean) {
        if (enabled == _uiState.value.tileConfig.caffeineLimitColor) return
        viewModelScope.launch {
            val newConfig = _uiState.value.tileConfig.copy(caffeineLimitColor = enabled)
            _uiState.update { it.copy(tileConfig = newConfig) }
            sendTileConfigToWatch(newConfig)
        }
    }

    private suspend fun sendTileConfigToWatch(config: TileConfig) {
        try {
            val nodeCount = wearableDataLayerManager.sendTileConfig(config)
            if (nodeCount > 0) {
                _events.emit(SettingsEvent.ShowSnackbar("Tile configuration sent to watch"))
            } else {
                _events.emit(SettingsEvent.ShowSnackbar(context.getString(R.string.tile_config_no_watch)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val message = e.message ?: "unknown"
            _events.emit(SettingsEvent.ShowSnackbar("Failed to sync tile config: $message"))
        }
    }
}
