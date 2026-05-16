package pl.dekrate.kofeino.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.domain.model.OfficialDrink
import timber.log.Timber
import javax.inject.Inject

data class OfficialDrinkUiState(
    val drinks: List<OfficialDrink> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchMode: Boolean = false
)

@HiltViewModel
class OfficialDrinkViewModel @Inject constructor(
    private val repository: OfficialDrinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialDrinkUiState())
    val uiState: StateFlow<OfficialDrinkUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadOfficialDrinks()
    }

    fun loadOfficialDrinks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSearchMode = false)
            Timber.d("Loading official drinks...")

            repository.getOfficialDrinks()
                .onSuccess { drinks ->
                    Timber.d("Loaded ${drinks.size} official drinks")
                    _uiState.value = _uiState.value.copy(
                        drinks = drinks,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load official drinks")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: context.getString(R.string.error_load_failed)
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchMode = query.isNotBlank())

        // Debounce 500ms — nie wysyłaj zapytania przy każdej literze
        searchJob?.cancel()
        if (query.isBlank()) {
            loadOfficialDrinks()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchMode = query.isNotBlank())
        if (query.isBlank()) {
            loadOfficialDrinks()
            return
        }
        searchJob?.cancel()
        performSearch(query)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Timber.d("Searching official drinks for: $query")

            repository.searchOfficialDrinks(query)
                .onSuccess { drinks ->
                    Timber.d("Found ${drinks.size} drinks for: $query")
                    _uiState.value = _uiState.value.copy(
                        drinks = drinks,
                        isLoading = false,
                        error = if (drinks.isEmpty()) context.getString(R.string.error_no_results) else null
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Search failed for: $query")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: context.getString(R.string.error_search_failed)
                    )
                }
        }
    }

    fun refresh() {
        if (_uiState.value.isSearchMode && _uiState.value.searchQuery.isNotBlank()) {
            performSearch(_uiState.value.searchQuery)
        } else {
            loadOfficialDrinks()
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearchMode = false)
        loadOfficialDrinks()
    }
}
