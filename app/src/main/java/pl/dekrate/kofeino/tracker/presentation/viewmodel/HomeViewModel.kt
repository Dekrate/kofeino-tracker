package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val dateLabel: String = "",
    val totalCaffeineMg: Int = 0,
    val progress: Float = 0f,
    val isLimitExceeded: Boolean = false,
    val todayIntakes: List<CaffeineIntake> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    /** Emits start-of-today millis, re-emitting at midnight when the date rolls over. */
    private val todayStartMillis: StateFlow<Long> = todayStartOfDayFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, getStartOfToday())

    val uiState: StateFlow<HomeUiState> = todayStartMillis
        .flatMapLatest { startMillis ->
            combine(
                repository.getIntakesForDate(startMillis),
                repository.getTotalCaffeineForDate(startMillis)
            ) { intakes, total ->
                HomeUiState(
                    dateLabel = formatDateLabel(startMillis),
                    totalCaffeineMg = total,
                    progress = (total / SAFE_LIMIT_MG.toFloat()).coerceIn(0f, 1f),
                    isLimitExceeded = total > SAFE_LIMIT_MG,
                    todayIntakes = intakes,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    /** Returns a flow that emits the start-of-today timestamp, re-emitting at midnight. */
    private fun todayStartOfDayFlow(): Flow<Long> = flow {
        while (true) {
            val startOfToday = getStartOfToday()
            emit(startOfToday)
            // Calculate delay until the next midnight boundary (+ 1s buffer)
            val now = System.currentTimeMillis()
            val nextMidnight = startOfToday + MILLIS_PER_DAY
            val delayMs = nextMidnight - now + 1000L
            delay(delayMs.coerceAtLeast(60_000L))
        }
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun formatDateLabel(millis: Long): String {
        val today = getStartOfToday()
        return if (millis == today) {
            // Return ISO-like date; UI layer handles localization
            SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
        } else {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
