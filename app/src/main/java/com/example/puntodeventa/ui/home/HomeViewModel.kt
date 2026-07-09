package com.example.puntodeventa.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.data.repository.MenuRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val menuItems: List<MenuItem> = emptyList(),
    val isDialogOpen: Boolean = false,
    val editingItem: MenuItem? = null
)

class HomeViewModel(private val repository: MenuRepository) : ViewModel() {

    // ── Dialog UI state (transient, in-memory) ────────────────────────────────
    private val _dialogState = MutableStateFlow(
        DialogState(isOpen = false, editingItem = null)
    )

    // ── Combine persisted items + transient dialog state ──────────────────────
    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.menuItems,
            _dialogState
        ) { items, dialog ->
            HomeUiState(
                menuItems    = items,
                isDialogOpen = dialog.isOpen,
                editingItem  = dialog.editingItem
            )
        }.stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000),
            initialValue   = HomeUiState()
        )

    fun openDialog() {
        _dialogState.update { it.copy(isOpen = true, editingItem = null) }
    }

    fun openEditDialog(item: MenuItem) {
        _dialogState.update { it.copy(isOpen = true, editingItem = item) }
    }

    fun dismissDialog() {
        _dialogState.update { it.copy(isOpen = false, editingItem = null) }
    }

    fun saveMenu(emoji: String, name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || emoji.isBlank()) return

        val editingItem = _dialogState.value.editingItem
        val item = MenuItem(
            id    = editingItem?.id ?: UUID.randomUUID().toString(),
            emoji = emoji,
            name  = trimmedName
        )
        viewModelScope.launch { repository.insert(item) }
        _dialogState.update { it.copy(isOpen = false, editingItem = null) }
    }

    fun deleteMenu(id: String) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: MenuRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private data class DialogState(val isOpen: Boolean, val editingItem: MenuItem?)
}
