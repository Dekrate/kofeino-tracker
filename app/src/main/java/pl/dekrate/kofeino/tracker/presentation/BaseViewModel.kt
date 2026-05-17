package pl.dekrate.kofeino.tracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing a generic [UiState] holder and coroutine scope.
 *
 * @param T The success data type for this screen's state.
 */
abstract class BaseViewModel<T> : ViewModel() {

    protected val _uiState = MutableStateFlow<UiState<T>>(UiState.Loading)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

    protected fun emitSuccess(data: T) {
        _uiState.value = UiState.Success(data)
    }

    protected fun emitError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    protected fun emitLoading() {
        _uiState.value = UiState.Loading
    }

    /**
     * Launch a coroutine in the ViewModel scope with automatic error handling.
     */
    protected fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                emitError(e.message ?: "Unknown error")
            }
        }
    }
}
