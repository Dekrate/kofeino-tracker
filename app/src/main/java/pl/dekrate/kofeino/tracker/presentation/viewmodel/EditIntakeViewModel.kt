package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the EditIntake screen.
 *
 * Loads a single [CaffeineIntake] by ID, allows adjusting
 * caffeine mg (±5) and volume ml (±10), then saves or deletes.
 */
@HiltViewModel
class EditIntakeViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditIntakeUiState())
    val uiState: StateFlow<EditIntakeUiState> = _uiState.asStateFlow()

    /**
     * Load intake from the repository by its ID.
     * Should be called once when the screen is first composed.
     */
    fun loadIntake(intakeId: Long) {
        Timber.d("Loading intake id=$intakeId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val intake = repository.getIntakeById(intakeId)
                if (intake != null) {
                    _uiState.update {
                        EditIntakeUiState(
                            isLoading = false,
                            intake = intake,
                            drinkName = intake.drinkName,
                            caffeineMg = intake.caffeineMg,
                            volumeMl = intake.volumeMl
                        )
                    }
                } else {
                    Timber.w("Intake $intakeId not found")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Intake not found")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load intake $intakeId")
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load intake")
                }
            }
        }
    }

    /** Increase or decrease caffeine mg by [delta] (typically ±5). Clamped to 0. */
    fun updateCaffeineMg(delta: Int) {
        _uiState.update { state ->
            state.copy(caffeineMg = (state.caffeineMg + delta).coerceAtLeast(0))
        }
    }

    /** Increase or decrease volume ml by [delta] (typically ±10). Clamped to 0. */
    fun updateVolumeMl(delta: Int) {
        _uiState.update { state ->
            state.copy(volumeMl = (state.volumeMl + delta).coerceAtLeast(0))
        }
    }

    /**
     * Save the modified intake.
     * @param onComplete Called after successful save (UI navigates back).
     * @param onError Called if save fails (UI re-enables buttons).
     */
    fun save(onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        val state = _uiState.value
        val intake = state.intake ?: return

        Timber.d("Saving intake id=${intake.id}: ${state.caffeineMg} mg, ${state.volumeMl} ml")
        viewModelScope.launch {
            try {
                repository.updateIntake(
                    intake.copy(caffeineMg = state.caffeineMg, volumeMl = state.volumeMl)
                )
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save intake")
                _uiState.update { it.copy(error = "Failed to save changes") }
                onError()
            }
        }
    }

    /**
     * Delete the intake.
     * @param onComplete Called after successful deletion (UI navigates back).
     * @param onError Called if deletion fails (UI re-enables buttons).
     */
    fun delete(onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        val intake = _uiState.value.intake ?: return

        Timber.d("Deleting intake id=${intake.id}")
        viewModelScope.launch {
            try {
                repository.deleteIntake(intake)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete intake")
                _uiState.update { it.copy(error = "Failed to delete intake") }
                onError()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Convenience to check if current values differ from original. */
    fun hasChanges(): Boolean {
        val s = _uiState.value
        val intake = s.intake ?: return false
        return s.caffeineMg != intake.caffeineMg || s.volumeMl != intake.volumeMl
    }
}

data class EditIntakeUiState(
    val isLoading: Boolean = true,
    val intake: CaffeineIntake? = null,
    val drinkName: String = "",
    val caffeineMg: Int = 0,
    val volumeMl: Int = 0,
    val error: String? = null
)
