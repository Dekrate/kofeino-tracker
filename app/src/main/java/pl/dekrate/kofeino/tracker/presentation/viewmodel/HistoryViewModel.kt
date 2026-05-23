package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import timber.log.Timber
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val dateIntakes: List<CaffeineIntake> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val dateLabel: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: CaffeineRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_DATE)?.let {
            try { LocalDate.parse(it) } catch (e: IllegalArgumentException) {
                Timber.w(e, "Failed to parse saved date '%s', falling back to today", it)
                getTodayDate()
            }
        } ?: getTodayDate()
    )
    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Persist selected date to SavedStateHandle on every change (survives process death)
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                savedStateHandle[KEY_SELECTED_DATE] = date.toString()
            }
        }

        _selectedDate
            .flatMapLatest { date ->
                combine(
                    repository.getIntakesForDate(date),
                    repository.getTotalCaffeineForDate(date)
                ) { intakes, total ->
                    HistoryUiState(
                        selectedDate = date,
                        dateIntakes = intakes,
                        totalCaffeineMg = total,
                        dateLabel = formatDateLabel(date),
                        isLoading = false
                    )
                }
            }
            .catch { e ->
                emit(
                    HistoryUiState(
                        selectedDate = _selectedDate.value,
                        error = e.message ?: "Failed to load history",
                        isLoading = false
                    )
                )
            }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }

    // --- Date navigation ---

    fun previousDay() {
        _selectedDate.update { it.minus(1, DateTimeUnit.DAY) }
    }

    fun nextDay() {
        _selectedDate.update { it.plus(1, DateTimeUnit.DAY) }
    }

    fun goToToday() {
        _selectedDate.value = getTodayDate()
    }

    fun isToday(): Boolean {
        return getTodayDate() == _selectedDate.value
    }

    fun isYesterday(): Boolean {
        return getTodayDate().minus(1, DateTimeUnit.DAY) == _selectedDate.value
    }

    // --- Error handling ---

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getTodayDate(): LocalDate {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    private fun formatDateLabel(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
        return java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth).format(formatter)
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
        private const val KEY_SELECTED_DATE = "selectedDate"
    }
}
