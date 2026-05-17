package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.dekrate.kofeino.tracker.data.local.LanguagePreferences
import pl.dekrate.kofeino.tracker.data.local.ThemePreferences
import javax.inject.Inject

data class SettingsUiState(
    val currentLanguage: String = LanguagePreferences.DEFAULT_LANGUAGE,
    val languageChanged: Boolean = false,
    val currentThemeMode: String = ThemePreferences.DEFAULT_THEME_MODE,
    val themeChanged: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = languagePreferences.getLanguage(),
            currentThemeMode = themePreferences.getThemeMode()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Persists the selected language and signals the UI to recreate the activity.
     * If the same language is selected, this is a no-op.
     */
    fun setLanguage(lang: String) {
        if (lang == _uiState.value.currentLanguage) return
        languagePreferences.setLanguage(lang)
        _uiState.update {
            it.copy(currentLanguage = lang, languageChanged = true)
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
        themePreferences.setThemeMode(mode)
        _uiState.update {
            it.copy(currentThemeMode = mode, themeChanged = true)
        }
    }

    /**
     * Resets the themeChanged flag after the UI has handled the recreate side-effect.
     */
    fun consumeThemeChanged() {
        _uiState.update { it.copy(themeChanged = false) }
    }
}
