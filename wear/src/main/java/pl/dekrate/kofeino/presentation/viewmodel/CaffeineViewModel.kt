package pl.dekrate.kofeino.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.data.local.CaffeinePreferences
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val repository: CaffeineRepository,
    private val caffeinePreferences: CaffeinePreferences,
    @ApplicationContext private val context: Context
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
            val safeLimitMg = caffeinePreferences.getLimitMg()
            CaffeineUiState(
                selectedDateMillis = dateMillis,
                dateIntakes = intakes,
                totalCaffeineMg = total,
                isLimitExceeded = total > safeLimitMg,
                progress = (total / safeLimitMg.toFloat()).coerceIn(0f, 1f),
                safeLimitMg = safeLimitMg,
                drinks = drinks,
                dateLabel = dateLabel
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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

    fun addDrink(drink: DrinkEntity, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        Timber.d("Adding drink: ${drink.name} (${drink.caffeineMg} mg)")
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
                Timber.e(e, "Failed to add intake")
                _uiState.update { it.copy(error = context.getString(R.string.error_add_failed)) }
                onError()
            }
        }
    }

    /**
     * Aktualizuje wpis.
     * onComplete — wywoływane po udanym zapisie (UI może nawigować wstecz).
     * onError — wywoływane po błędzie (UI może odblokować przyciski).
     */
    fun updateIntake(intake: CaffeineIntake, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        Timber.d("Updating intake id=${intake.id}")
        viewModelScope.launch {
            try {
                repository.updateIntake(intake)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update intake")
                _uiState.update { it.copy(error = context.getString(R.string.error_save_failed)) }
                onError()
            }
        }
    }

    /**
     * Usuwa wpis.
     * onComplete — wywoływane po udanym usunięciu.
     * onError — wywoływane po błędzie (UI może odblokować przyciski).
     */
    fun deleteIntake(intake: CaffeineIntake, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        Timber.d("Deleting intake id=${intake.id}")
        viewModelScope.launch {
            try {
                repository.deleteIntake(intake)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete intake")
                _uiState.update { it.copy(error = context.getString(R.string.error_delete_failed)) }
                onError()
            }
        }
    }

    suspend fun getIntakeById(id: Long): CaffeineIntake? {
        return repository.getIntakeById(id)
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
        val yesterday = startOfDayOffset(today, -1)
        val tomorrow = startOfDayOffset(today, +1)
        return when (millis) {
            today -> context.getString(R.string.today)
            yesterday -> context.getString(R.string.yesterday)
            tomorrow -> context.getString(R.string.tomorrow)
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    /** Zwraca początek dnia oddalonego o `days` od podanego timestampu (DST-safe). */
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
        /** Default adult safe limit (EFSA/FDA). Kept for reference / legacy use. */
        const val DEFAULT_SAFE_LIMIT_MG = 400
    }
}

data class CaffeineUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val dateIntakes: List<CaffeineIntake> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val isLimitExceeded: Boolean = false,
    val progress: Float = 0f,
    /** The current safe daily limit in mg (from user's selected profile). */
    val safeLimitMg: Int = CaffeineViewModel.DEFAULT_SAFE_LIMIT_MG,
    val drinks: List<DrinkEntity> = emptyList(),
    val dateLabel: String = "",
    val error: String? = null
)
