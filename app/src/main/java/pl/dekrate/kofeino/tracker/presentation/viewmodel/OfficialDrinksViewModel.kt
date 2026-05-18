package pl.dekrate.kofeino.tracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink
import timber.log.Timber
import javax.inject.Inject

sealed interface OfficialDrinksError {
    data object SearchFailed : OfficialDrinksError
    data object ImportFailed : OfficialDrinksError
}

data class OfficialDrinksUiState(
    val drinks: List<OfficialDrink> = emptyList(),
    val importedDrinkNames: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: OfficialDrinksError? = null,
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val importingBarcode: String? = null,
    val successMessage: String? = null
)

private const val SEARCH_DEBOUNCE_MS = 500L

@HiltViewModel
class OfficialDrinksViewModel @Inject constructor(
    private val officialRepository: OfficialDrinkRepository,
    private val caffeineRepository: CaffeineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialDrinksUiState())
    val uiState: StateFlow<OfficialDrinksUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe custom drinks to know which official ones are already imported
        caffeineRepository.getAllDrinks()
            .onEach { drinks ->
                _uiState.update {
                    it.copy(importedDrinkNames = drinks.map { d -> d.name }.toSet())
                }
            }
            .launchIn(viewModelScope)

        loadOfficialDrinks()
    }

    fun loadOfficialDrinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSearchMode = false) }
            Timber.d("Loading official drinks...")

            try {
                officialRepository.getOfficialDrinks()
                    .onSuccess { drinks ->
                        Timber.d("Loaded ${drinks.size} official drinks")
                        _uiState.update {
                            it.copy(drinks = drinks, isLoading = false)
                        }
                    }
                    .onFailure { error ->
                        // Empty cache is expected on first launch — don't show error
                        Timber.i("No cached official drinks available: ${error.message}")
                        _uiState.update {
                            it.copy(isLoading = false, drinks = emptyList())
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading official drinks")
                _uiState.update {
                    it.copy(isLoading = false, drinks = emptyList())
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        // Clear error immediately so user doesn't see stale error while typing
        _uiState.update { it.copy(searchQuery = query, isSearchMode = query.isNotBlank(), error = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadOfficialDrinks()
            return
        }
        val currentQuery = query
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            // Only execute if this query is still current
            if (_uiState.value.searchQuery == currentQuery) {
                performSearch(currentQuery)
            }
        }
    }

    fun searchImmediate(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchMode = query.isNotBlank()) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadOfficialDrinks()
            return
        }
        val currentQuery = query
        searchJob = viewModelScope.launch {
            performSearch(currentQuery)
        }
    }

    fun refresh() {
        if (_uiState.value.isSearchMode && _uiState.value.searchQuery.isNotBlank()) {
            val query = _uiState.value.searchQuery
            viewModelScope.launch { performSearch(query) }
        } else {
            loadOfficialDrinks()
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", isSearchMode = false) }
        loadOfficialDrinks()
    }

    fun importAsDrink(official: OfficialDrink) {
        _uiState.update { it.copy(importingBarcode = official.barcode) }
        viewModelScope.launch {
            try {
                val caffeineMg = (official.caffeineMgPer100ml).toInt().coerceAtLeast(0)
                caffeineRepository.addDrink(
                    DrinkEntity(
                        name = official.name,
                        caffeineMg = caffeineMg,
                        volumeMl = 100
                    )
                )
                // Success — update state (importedDrinkNames updates via Flow)
                _uiState.update {
                    it.copy(
                        importingBarcode = null,
                        successMessage = official.name
                    )
                }
                Timber.d("Imported official drink: ${official.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to import drink: ${official.name}")
                _uiState.update {
                    it.copy(importingBarcode = null, error = OfficialDrinksError.ImportFailed)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private suspend fun performSearch(query: String) {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }
            officialRepository.searchOfficialDrinks(query)
                .onSuccess { drinks ->
                    Timber.d("Found ${drinks.size} drinks for: $query")
                    _uiState.update {
                        it.copy(drinks = drinks, isLoading = false)
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Search failed for: $query")
                    _uiState.update {
                        it.copy(isLoading = false, error = OfficialDrinksError.SearchFailed)
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Search crashed for: $query")
            _uiState.update {
                it.copy(isLoading = false, error = OfficialDrinksError.SearchFailed)
            }
        }
    }
}
