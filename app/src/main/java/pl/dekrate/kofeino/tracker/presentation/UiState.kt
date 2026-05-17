package pl.dekrate.kofeino.tracker.presentation

/**
 * Base sealed interface for all UI states.
 * Each screen should define its own state implementing this.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: @UnsafeVariance T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
