package pl.dekrate.kofeino.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddDrinkViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    companion object {
        private const val RECENT_INTAKES_LIMIT = 5
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(AddDrinkUiState())
    val uiState: StateFlow<AddDrinkUiState> = _uiState.asStateFlow()

    init {
        Timber.d("AddDrinkViewModel initialized")

        val searchResults: Flow<List<DrinkEntity>> = _searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.getAllDrinks()
                } else {
                    repository.searchDrinks(query)
                }
            }

        combine(
            _searchQuery,
            searchResults,
            repository.getRecentIntakes(RECENT_INTAKES_LIMIT)
        ) { query, drinks, recentIntakes ->
            AddDrinkUiState(
                searchQuery = query,
                drinks = drinks,
                recentIntakes = recentIntakes
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    @Suppress("TooGenericExceptionCaught") // Catch-all to prevent UI crashes — Room/sync errors are logged via Timber
    fun addDrink(drink: DrinkEntity, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                Timber.e(e, "Failed to add intake from AddDrinkViewModel")
                onError()
            }
        }
    }
}

data class AddDrinkUiState(
    val searchQuery: String = "",
    val drinks: List<DrinkEntity> = emptyList(),
    val recentIntakes: List<CaffeineIntake> = emptyList()
)
