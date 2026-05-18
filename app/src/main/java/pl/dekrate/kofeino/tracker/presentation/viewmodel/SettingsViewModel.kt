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
    val themeChanged: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 *
 * Uses [DataStorePreferences] (DataStore-backed) for persistence.
 * The DataStore is the source of truth; SharedPreferences mirror exists
 * only for pre-Hilt [android.app.Application.attachBaseContext] reads.
 *
 * **Thread safety**: All preference reads go through the in-memory cache
 * after initial DataStore warm-up. Writes are dispatched via [viewModelScope].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: DataStorePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Initialize state from preferences (async warm-up, then cache hit)
        viewModelScope.launch {
            val lang = preferences.observeLanguage().first()
            val theme = preferences.observeThemeMode().first()
            _uiState.update {
                it.copy(currentLanguage = lang, currentThemeMode = theme)
            }
        }
    }

    /**
     * Persists the selected language and signals the UI to recreate the activity.
     * If the same language is selected, this is a no-op.
     */
    fun setLanguage(lang: String) {
        if (lang == _uiState.value.currentLanguage) return
        viewModelScope.launch {
            preferences.setLanguage(lang)
            _uiState.update {
                it.copy(currentLanguage = lang, languageChanged = true)
            }
        }
    }

    /**
     * Resets the languageChanged flag after the UI has handled the recreate side-effect.
     * Prevents infinite loops when the ViewModel survives across the activity cycle.
     */
    fun consumeLanguageChanged() {
        _uiState.update { it.copy(languageChanged = false) }
    }

    /**
     * Persists the selected theme mode and signals the UI to recreate the activity.
     * If the same theme mode is selected, this is a no-op.
     */
    fun setThemeMode(mode: String) {
        if (mode == _uiState.value.currentThemeMode) return
        viewModelScope.launch {
            preferences.setThemeMode(mode)
            _uiState.update {
                it.copy(currentThemeMode = mode, themeChanged = true)
            }
        }
    }

    /**
     * Resets the themeChanged flag after the UI has handled the recreate side-effect.
     */
    fun consumeThemeChanged() {
        _uiState.update { it.copy(themeChanged = false) }
    }
}
