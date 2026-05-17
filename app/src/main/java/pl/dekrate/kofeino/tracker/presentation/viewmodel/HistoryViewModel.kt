package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val dateIntakes: List<CaffeineIntake> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val dateLabel: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(getStartOfToday())
    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        _selectedDateMillis
            .flatMapLatest { dateMillis ->
                combine(
                    repository.getIntakesForDate(dateMillis),
                    repository.getTotalCaffeineForDate(dateMillis)
                ) { intakes, total ->
                    HistoryUiState(
                        selectedDateMillis = dateMillis,
                        dateIntakes = intakes,
                        totalCaffeineMg = total,
                        dateLabel = formatDateLabel(dateMillis),
                        isLoading = false
                    )
                }
            }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }

    // --- Date navigation ---

    fun previousDay() {
        _selectedDateMillis.update { addDays(it, -1) }
    }

    fun nextDay() {
        _selectedDateMillis.update { addDays(it, 1) }
    }

    fun goToToday() {
        _selectedDateMillis.value = getStartOfToday()
    }

    fun isToday(): Boolean {
        return getStartOfToday() == _selectedDateMillis.value
    }

    // --- Error handling ---

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Package-private for testing, mirrors the repository's dayBounds logic
    fun getStartOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    private fun formatDateLabel(millis: Long): String {
        val today = getStartOfToday()
        val yesterday = startOfDayOffset(today, -1)
        return when (millis) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    /** Returns start of day offset by `days` from given timestamp (DST-safe). */
    private fun startOfDayOffset(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
    }
}
