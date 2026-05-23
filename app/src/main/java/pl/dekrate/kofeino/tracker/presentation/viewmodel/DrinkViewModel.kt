package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import timber.log.Timber
import javax.inject.Inject

/** Errors that can occur on the AddDrink screen. */
sealed interface DrinkError {
    data object AddIntakeFailed : DrinkError
}

/**
 * ViewModel for the AddDrink screen.
 *
 * Exposes all saved drinks and provides a quick-log method
 * that creates a [CaffeineIntake] with the drink's default serving.
 */
@HiltViewModel
class DrinkViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _allDrinks = MutableStateFlow<List<DrinkEntity>>(emptyList())
    val allDrinks: StateFlow<List<DrinkEntity>> = _allDrinks.asStateFlow()

    private val _uiState = MutableStateFlow(DrinkUiState())
    val uiState: StateFlow<DrinkUiState> = _uiState.asStateFlow()

    init {
        Timber.d("DrinkViewModel initialized")
        repository.getAllDrinks()
            .onEach { drinks ->
                Timber.d("Drinks updated: ${drinks.size} total")
                _allDrinks.value = drinks
            }
            .launchIn(viewModelScope)
    }

    /**
     * Quick-log a drink as a new intake entry with its default serving.
     *
     * @param drink The drink to log.
     * @param onComplete Called after the intake is saved (UI may navigate back).
     * @param onError Called if the save fails (UI may re-enable buttons).
     */
    fun logDrink(drink: DrinkEntity, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        Timber.d("Logging drink: ${drink.name} (${drink.caffeineMg} mg, ${drink.volumeMl} ml)")
        viewModelScope.launch {
            try {
                val intake = CaffeineIntake(
                    drinkId = drink.id,
                    drinkName = drink.name,
                    caffeineMg = drink.caffeineMg,
                    volumeMl = drink.volumeMl,
                    timestamp = System.currentTimeMillis()
                )
                repository.addIntake(intake)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to log drink")
                _uiState.update { it.copy(error = DrinkError.AddIntakeFailed) }
                onError()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DrinkUiState(
    val error: DrinkError? = null
)
