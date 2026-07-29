package com.example.puntodeventa.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel that exposes the current [AppTheme] as a [StateFlow] and handles
 * theme selection events by persisting them through [ThemePreferencesRepository].
 *
 * - Collects from repository.themeFlow with error fallback to DEFAULT (Req 3.3, 3.4).
 * - Dispatches theme changes to the repository on user selection (Req 10.2).
 */
class ThemeViewModel(
    private val repository: ThemePreferencesRepository
) : ViewModel() {

    /**
     * Current theme exposed as a StateFlow.
     * Falls back to [AppTheme.DEFAULT] if the repository emits an error (Req 3.4).
     * Uses [SharingStarted.Eagerly] so subscribers always receive the latest value immediately (Req 3.3).
     */
    val currentTheme: StateFlow<AppTheme> = repository.themeFlow
        .catch { emit(AppTheme.DEFAULT) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppTheme.DEFAULT
        )

    /**
     * Saves the selected [theme] to the repository.
     * The repository's themeFlow will emit the new value to all collectors (Req 10.2).
     */
    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.saveTheme(theme)
        }
    }

    /**
     * Factory for creating [ThemeViewModel] instances with the required [ThemePreferencesRepository].
     */
    class Factory(
        private val repository: ThemePreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ThemeViewModel(repository) as T
    }
}
