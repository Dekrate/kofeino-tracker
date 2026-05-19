package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
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
    val notifEveningEnabled: Boolean = DataStorePreferences.DEFAULT_NOTIF_EVENING
)

/**
 * ViewModel for the Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: DataStorePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lang = preferences.observeLanguage().first()
            val theme = preferences.observeThemeMode().first()
            val notifLive = preferences.observeNotificationLiveEnabled().first()
            val notifMorning = preferences.observeNotificationMorningEnabled().first()
            val notifRegular = preferences.observeNotificationRegularEnabled().first()
            val notifEvening = preferences.observeNotificationEveningEnabled().first()
            _uiState.update {
                it.copy(
                    currentLanguage = lang, currentThemeMode = theme,
                    notifLiveEnabled = notifLive,
                    notifMorningEnabled = notifMorning,
                    notifRegularEnabled = notifRegular,
                    notifEveningEnabled = notifEvening
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
}
