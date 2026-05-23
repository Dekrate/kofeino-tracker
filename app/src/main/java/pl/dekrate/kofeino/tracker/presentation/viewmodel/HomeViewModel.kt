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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val dateLabel: String = "",
    val totalCaffeineMg: Int = 0,
    val safeLimitMg: Int = HomeViewModel.SAFE_LIMIT_MG,
    val progress: Float = 0f,
    val isLimitExceeded: Boolean = false,
    val todayIntakes: List<CaffeineIntake> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CaffeineRepository,
    private val preferences: DataStorePreferences
) : ViewModel() {

    /** Emits today's date, re-emitting at midnight when the date rolls over. */
    private val todayStart: Flow<LocalDate> = todayStartFlow()

    val uiState: StateFlow<HomeUiState> = todayStart
        .flatMapLatest { today ->
            combine(
                repository.getIntakesForDate(today),
                repository.getTotalCaffeineForDate(today),
                preferences.observeCaffeineLimitMg()
            ) { intakes, total, safeLimit ->
                HomeUiState(
                    dateLabel = formatDateLabel(today),
                    totalCaffeineMg = total,
                    safeLimitMg = safeLimit,
                    progress = if (safeLimit > 0) (total.toFloat() / safeLimit.toFloat()).coerceIn(0f, 1f) else 0f,
                    isLimitExceeded = safeLimit > 0 && total > safeLimit,
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

    /** Returns a flow that emits today's date, re-emitting at midnight. */
    private fun todayStartFlow(): Flow<LocalDate> = flow {
        while (true) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            emit(today)
            val nextMidnight = getNextMidnight()
            val delayMs = nextMidnight - System.currentTimeMillis() + MIDNIGHT_BUFFER_MS
            delay(delayMs.coerceAtLeast(MIN_DELAY_MS))
        }
    }

    /** Returns the start of the next calendar day (DST-safe via Calendar). */
    // Package-private for testing
    fun getNextMidnight(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun formatDateLabel(date: LocalDate): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (date == today) {
            val formatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.getDefault())
            java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth).format(formatter)
        } else {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
            java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth).format(formatter)
        }
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
        private const val MIDNIGHT_BUFFER_MS = 1000L
        private const val MIN_DELAY_MS = 1000L
    }
}
