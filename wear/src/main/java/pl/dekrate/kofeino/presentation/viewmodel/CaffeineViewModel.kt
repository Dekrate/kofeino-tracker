package pl.dekrate.kofeino.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaffeineViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(getStartOfToday())
    private val _uiState = MutableStateFlow(CaffeineUiState())
    val uiState: StateFlow<CaffeineUiState> = _uiState.asStateFlow()

    init {
        Timber.d("CaffeineViewModel initialized")

        // Transform date changes into date-specific flows
        val dateIntakesFlow = _selectedDateMillis.flatMapLatest { dateMillis ->
            repository.getIntakesForDate(dateMillis)
        }

        val dateTotalFlow = _selectedDateMillis.flatMapLatest { dateMillis ->
            repository.getTotalCaffeineForDate(dateMillis)
        }

        // Combine everything into UI state
        combine(
            _selectedDateMillis,
            dateIntakesFlow,
            dateTotalFlow,
            repository.getAllDrinks()
        ) { dateMillis, intakes, total, drinks ->
            val dateLabel = formatDateLabel(dateMillis)
            CaffeineUiState(
                selectedDateMillis = dateMillis,
                dateIntakes = intakes,
                totalCaffeineMg = total,
                isLimitExceeded = total > SAFE_LIMIT_MG,
                progress = (total / SAFE_LIMIT_MG.toFloat()).coerceIn(0f, 1f),
                drinks = drinks,
                dateLabel = dateLabel
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
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

    // --- Intake operations ---

    fun addDrink(drink: DrinkEntity) {
        Timber.d("Adding drink: ${drink.name} (${drink.caffeineMg} mg)")
        viewModelScope.launch {
            val intake = CaffeineIntake(
                drinkId = drink.id,
                drinkName = drink.name,
                caffeineMg = drink.caffeineMg,
                volumeMl = drink.volumeMl,
                timestamp = System.currentTimeMillis()
            )
            repository.addIntake(intake)
        }
    }

    fun updateIntake(intake: CaffeineIntake) {
        Timber.d("Updating intake id=${intake.id}")
        viewModelScope.launch {
            repository.updateIntake(intake)
        }
    }

    fun deleteIntake(intake: CaffeineIntake) {
        Timber.d("Deleting intake id=${intake.id}")
        viewModelScope.launch {
            repository.deleteIntake(intake)
        }
    }

    // --- Utility ---

    private fun getStartOfToday(): Long {
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
        return when (millis) {
            today -> "Dzisiaj"
            today - 86_400_000L -> "Wczoraj"
            today + 86_400_000L -> "Jutro"
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("pl")).format(Date(millis))
        }
    }

    companion object {
        const val SAFE_LIMIT_MG = 400
    }
}

data class CaffeineUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val dateIntakes: List<CaffeineIntake> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val isLimitExceeded: Boolean = false,
    val progress: Float = 0f,
    val drinks: List<DrinkEntity> = emptyList(),
    val dateLabel: String = ""
)
