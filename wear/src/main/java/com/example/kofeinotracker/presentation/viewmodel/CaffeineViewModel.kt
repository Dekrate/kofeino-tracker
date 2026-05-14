package com.example.kofeinotracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofeinotracker.data.repository.CaffeineRepository
import com.example.kofeinotracker.domain.model.CaffeineDrink
import com.example.kofeinotracker.domain.model.CaffeineIntake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaffeineViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaffeineUiState())
    val uiState: StateFlow<CaffeineUiState> = _uiState

    init {
        combine(
            repository.getTodayIntakes(),
            repository.getTodayTotalCaffeine()
        ) { intakes, total ->
            intakes to total
        }.onEach { (intakes, total) ->
            _uiState.update {
                it.copy(
                    todayIntakes = intakes,
                    totalCaffeineMg = total,
                    isLimitExceeded = total > SAFE_LIMIT_MG,
                    progress = (total / SAFE_LIMIT_MG.toFloat()).coerceIn(0f, 1f)
                )
            }
        }.launchIn(viewModelScope)
    }

    fun addDrink(drink: CaffeineDrink) {
        viewModelScope.launch {
            val intake = CaffeineIntake(
                drinkName = drink.id,
                caffeineMg = drink.caffeineMg,
                volumeMl = drink.volumeMl,
                timestamp = System.currentTimeMillis()
            )
            repository.addIntake(intake)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
    }
}

data class CaffeineUiState(
    val todayIntakes: List<CaffeineIntake> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val isLimitExceeded: Boolean = false,
    val progress: Float = 0f
)
