package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import timber.log.Timber
import javax.inject.Inject

sealed interface ManageDrinksError {
    data object LoadFailed : ManageDrinksError
    data object DeleteFailed : ManageDrinksError
    data object DefaultDrinkNotDeletable : ManageDrinksError
    data object SaveFailed : ManageDrinksError
}

data class ManageDrinksUiState(
    val drinks: List<DrinkEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: ManageDrinksError? = null,
    val deleteConfirmation: DrinkEntity? = null,
    val editDrink: DrinkEntity? = null,
    val showAddForm: Boolean = false
)

@HiltViewModel
class ManageDrinksViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageDrinksUiState())
    val uiState: StateFlow<ManageDrinksUiState> = _uiState.asStateFlow()

    init {
        repository.getAllDrinks()
            .onEach { drinks ->
                _uiState.update {
                    it.copy(drinks = drinks, isLoading = false)
                }
            }
            .catch { e ->
                Timber.e(e, "Failed to load drinks")
                _uiState.update {
                    it.copy(isLoading = false, error = ManageDrinksError.LoadFailed)
                }
            }
            .launchIn(viewModelScope)
    }

    // --- Delete ---

    fun requestDelete(drink: DrinkEntity) {
        if (drink.isDefault) {
            _uiState.update { it.copy(error = ManageDrinksError.DefaultDrinkNotDeletable) }
            return
        }
        _uiState.update { it.copy(deleteConfirmation = drink) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmation = null) }
    }

    fun confirmDelete() {
        val drink = _uiState.value.deleteConfirmation ?: return
        if (drink.isDefault) {
            _uiState.update {
                it.copy(
                    deleteConfirmation = null,
                    error = ManageDrinksError.DefaultDrinkNotDeletable
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteDrink(drink)
                _uiState.update { it.copy(deleteConfirmation = null) }
                Timber.d("Drink deleted: ${drink.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete drink")
                _uiState.update {
                    it.copy(deleteConfirmation = null, error = ManageDrinksError.DeleteFailed)
                }
            }
        }
    }

    // --- Add / Edit form ---

    fun showAddForm() {
        _uiState.update { it.copy(showAddForm = true, editDrink = null) }
    }

    fun showEditForm(drink: DrinkEntity) {
        _uiState.update { it.copy(editDrink = drink, showAddForm = true) }
    }

    fun dismissForm() {
        _uiState.update { it.copy(showAddForm = false, editDrink = null) }
    }

    fun saveDrink(name: String, caffeineMg: Int, volumeMl: Int) {
        viewModelScope.launch {
            try {
                val editDrink = _uiState.value.editDrink
                if (editDrink != null) {
                    repository.updateDrink(
                        editDrink.copy(name = name, caffeineMg = caffeineMg, volumeMl = volumeMl)
                    )
                } else {
                    repository.addDrink(
                        DrinkEntity(name = name, caffeineMg = caffeineMg, volumeMl = volumeMl)
                    )
                }
                _uiState.update { it.copy(showAddForm = false, editDrink = null) }
                Timber.d("Drink saved: $name ($caffeineMg mg, $volumeMl ml)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save drink")
                _uiState.update { it.copy(error = ManageDrinksError.SaveFailed) }
            }
        }
    }

    // --- Error handling ---

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
