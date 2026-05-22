package pl.dekrate.kofeino.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.dekrate.kofeino.data.local.CaffeinePreferences
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.domain.model.CaffeineLimitProfile
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePreferences: LanguagePreferences,
    private val caffeinePreferences: CaffeinePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Reactively combine both preference flows into UI state
        combine(
            caffeinePreferences.limitFlow,
            languagePreferences.languageFlow
        ) { _, language ->
            SettingsUiState(
                currentProfile = caffeinePreferences.getProfile(),
                customLimit = caffeinePreferences.getCustomLimit(),
                currentLanguage = language
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun setProfile(profile: CaffeineLimitProfile) {
        caffeinePreferences.setProfile(profile)
    }

    fun setCustomLimit(mg: Int) {
        val clamped = mg.coerceIn(
            CaffeinePreferences.MIN_CUSTOM_LIMIT,
            CaffeinePreferences.MAX_CUSTOM_LIMIT
        )
        caffeinePreferences.setCustomLimit(clamped)
    }

    fun setLanguage(lang: String) {
        languagePreferences.setLanguage(lang)
    }
}

data class SettingsUiState(
    val currentProfile: CaffeineLimitProfile = CaffeineLimitProfile.ADULT,
    val currentLanguage: String = LanguagePreferences.LANGUAGE_SYSTEM,
    val customLimit: Int = CaffeinePreferences.DEFAULT_CUSTOM_LIMIT
)
