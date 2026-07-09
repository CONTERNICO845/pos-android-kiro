package com.example.puntodeventa.ui.newproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.local.SelectionType
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ── Save result ───────────────────────────────────────────────────────────────

sealed interface SaveResult {
    data object Success : SaveResult
    data class Failure(val message: String) : SaveResult
}

// ── Draft state classes ───────────────────────────────────────────────────────

data class OptionDraft(
    val draftId: String = UUID.randomUUID().toString(),  // immutable; used as Compose key
    val optionName: String = "",
    val extraPriceText: String = "",                     // raw text from the Decimal field
    val optionNameError: String? = null,
    val optionPriceError: String? = null
) {
    /** Parsed price; 0.0 when field is blank or unparseable. */
    val extraPrice: Double get() = extraPriceText.toDoubleOrNull() ?: 0.0
}

data class GroupDraft(
    val draftId: String = UUID.randomUUID().toString(),  // immutable; used as Compose key
    val groupName: String = "",
    val selectionType: SelectionType = SelectionType.MULTIPLE_CHECKBOXES,
    val options: List<OptionDraft> = listOf(OptionDraft()),
    val groupNameError: String? = null
)

// ── UI State ──────────────────────────────────────────────────────────────────

data class NewProductUiState(
    // ── Basic product fields ──────────────────────────────────────────────────
    val emoji: String = "🛒",
    val name: String = "",
    val description: String = "",
    val priceText: String = "",                  // raw Decimal field text

    // ── Emoji picker ──────────────────────────────────────────────────────────
    val emojiPickerExpanded: Boolean = false,

    // ── Menu + Category dropdowns ─────────────────────────────────────────────
    val menus: List<MenuItem> = emptyList(),
    val selectedMenu: MenuItem? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,

    // ── Inline category creation ──────────────────────────────────────────────
    val showInlineCategoryCreation: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryNameError: String? = null,

    // ── Customization groups ──────────────────────────────────────────────────
    val groups: List<GroupDraft> = emptyList(),

    // ── Validation errors ─────────────────────────────────────────────────────
    val nameError: String? = null,
    val categoryError: String? = null,

    // ── Save lifecycle ────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val saveResult: SaveResult? = null,
    val error: String? = null,

    // ── Edit mode ─────────────────────────────────────────────────────────────
    val isEditMode: Boolean = false,
    val editProductId: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class NewProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val menuRepository: MenuRepository,
    private val database: AppDatabase
) : ViewModel() {

    // ── Internal mutable state ────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(NewProductUiState())

    // ── Exposed StateFlow ─────────────────────────────────────────────────────
    val uiState: StateFlow<NewProductUiState> = _uiState.asStateFlow()

    init {
        // Collect MenuRepository.menuItems; auto-select first on first emission
        menuRepository.menuItems
            .onEach { items ->
                _uiState.update { s ->
                    val current = s.selectedMenu
                    val newSelection = when {
                        current != null && items.any { it.id == current.id } -> current
                        else -> items.firstOrNull()
                    }
                    s.copy(menus = items, selectedMenu = newSelection)
                }
                // Load categories whenever the selected menu changes
                val menuId = _uiState.value.selectedMenu?.id
                if (menuId != null) loadCategories(menuId)
            }
            .launchIn(viewModelScope)
    }

    // ── Emoji ─────────────────────────────────────────────────────────────────

    /** Sets the selected emoji; ignores empty strings (Req 2.6). */
    fun updateEmoji(newEmoji: String) {
        if (newEmoji.isBlank()) return
        _uiState.update { it.copy(emoji = newEmoji, emojiPickerExpanded = false) }
    }

    /** Toggles the emoji picker open/closed (Req 2.2, 2.3). */
    fun toggleEmojiPicker() {
        _uiState.update { it.copy(emojiPickerExpanded = !it.emojiPickerExpanded) }
    }

    // ── Basic fields ──────────────────────────────────────────────────────────

    /** Updates product name; clears nameError immediately when name becomes non-empty (Req 3.6). */
    fun updateName(newName: String) {
        if (newName.length > 120) return
        _uiState.update { s ->
            s.copy(
                name = newName,
                nameError = if (newName.isNotEmpty()) null else s.nameError
            )
        }
    }

    /** Updates description; silently discards input beyond 500 chars (Req 3.2). */
    fun updateDescription(newDescription: String) {
        if (newDescription.length > 500) return
        _uiState.update { it.copy(description = newDescription) }
    }

    /**
     * Updates the raw price text.
     * Allows only digits, one decimal separator, and at most two decimal places (Req 3.3).
     * Silently ignores invalid characters.
     */
    fun updatePriceText(newText: String) {
        val sanitized = sanitizePriceInput(newText)
        _uiState.update { it.copy(priceText = sanitized) }
    }

    // ── Menu dropdown ─────────────────────────────────────────────────────────

    /** Selects a menu; reloads categories for the new menu (Req 4.5). */
    fun selectMenu(menu: MenuItem) {
        _uiState.update { it.copy(
            selectedMenu     = menu,
            categories       = emptyList(),
            selectedCategory = null
        ) }
        loadCategories(menu.id)
    }

    // ── Category dropdown ─────────────────────────────────────────────────────

    /** Selects an existing category (Req 5.1). */
    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = null) }
    }

    /** Shows the inline category creation UI (Req 5.3). */
    fun startInlineCategoryCreation() {
        _uiState.update { it.copy(showInlineCategoryCreation = true) }
    }

    /** Updates the inline draft category name; clears error on first character (Req 5.7). */
    fun updateInlineCategoryName(newName: String) {
        if (newName.length > 80) return
        _uiState.update { s ->
            s.copy(
                newCategoryName      = newName,
                newCategoryNameError = if (newName.isNotEmpty()) null else s.newCategoryNameError
            )
        }
    }

    /** Saves the inline draft category to the repository (Req 5.4). */
    fun submitNewCategory() {
        val state = _uiState.value
        val trimmedName = state.newCategoryName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(newCategoryNameError = "El nombre no puede estar vacío") }
            return
        }
        val menuId = state.selectedMenu?.id ?: return
        viewModelScope.launch {
            runCatching {
                val newCategory = Category(
                    id               = UUID.randomUUID().toString(),
                    name             = trimmedName,
                    associatedMenuId = menuId
                )
                categoryRepository.insert(newCategory)
                _uiState.update { s -> s.copy(
                    selectedCategory           = newCategory,
                    showInlineCategoryCreation = false,
                    newCategoryName            = "",
                    newCategoryNameError       = null
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Error al guardar categoría: ${e.message}") }
            }
        }
    }

    /** Cancels inline category creation without modifying selectedCategory (Req 5.8). */
    fun cancelNewCategory() {
        _uiState.update { it.copy(
            showInlineCategoryCreation = false,
            newCategoryName            = "",
            newCategoryNameError       = null
        ) }
    }

    // ── Group management ──────────────────────────────────────────────────────

    /** Appends a new GroupDraft with default values (Req 6.2). */
    fun addGroup() {
        _uiState.update { it.copy(groups = it.groups + GroupDraft()) }
    }

    /** Removes the GroupDraft at [index]; no-op if out of bounds (Req 10.8). */
    fun removeGroup(index: Int) {
        val groups = _uiState.value.groups
        if (index !in groups.indices) return
        _uiState.update { it.copy(groups = groups.toMutableList().also { l -> l.removeAt(index) }) }
    }

    /** Updates groupName of the GroupDraft at [index]; clears error on non-blank input (Req 6.4, 6.7). */
    fun updateGroupName(index: Int, newName: String) {
        if (newName.length > 120) return
        _uiState.update { s ->
            val updated = s.groups.mapIndexed { i, g ->
                if (i == index) g.copy(
                    groupName      = newName,
                    groupNameError = if (newName.trim().isNotEmpty()) null else g.groupNameError
                ) else g
            }
            s.copy(groups = updated)
        }
    }

    /** Updates selectionType of the GroupDraft at [index] only (Req 7.3). */
    fun updateGroupSelectionType(index: Int, type: SelectionType) {
        _uiState.update { s ->
            val updated = s.groups.mapIndexed { i, g ->
                if (i == index) g.copy(selectionType = type) else g
            }
            s.copy(groups = updated)
        }
    }

    // ── Option management ─────────────────────────────────────────────────────

    /** Appends a new OptionDraft to the group at [groupIndex] only (Req 8.2). */
    fun addOption(groupIndex: Int) {
        _uiState.update { s ->
            val updated = s.groups.mapIndexed { i, g ->
                if (i == groupIndex) g.copy(options = g.options + OptionDraft()) else g
            }
            s.copy(groups = updated)
        }
    }

    /** Removes the OptionDraft at [optionIndex] within [groupIndex]; no-op if out of bounds (Req 10.8). */
    fun removeOption(groupIndex: Int, optionIndex: Int) {
        _uiState.update { s ->
            val group = s.groups.getOrNull(groupIndex) ?: return@update s
            if (optionIndex !in group.options.indices) return@update s
            val updatedOptions = group.options.toMutableList().also { it.removeAt(optionIndex) }
            val updated = s.groups.mapIndexed { i, g ->
                if (i == groupIndex) g.copy(options = updatedOptions) else g
            }
            s.copy(groups = updated)
        }
    }

    /** Updates optionName; clears optionNameError on non-blank input (Req 8.7). */
    fun updateOptionName(groupIndex: Int, optionIndex: Int, newName: String) {
        if (newName.length > 120) return
        _uiState.update { s ->
            val updated = s.groups.mapIndexed { gi, g ->
                if (gi != groupIndex) return@mapIndexed g
                val updatedOpts = g.options.mapIndexed { oi, o ->
                    if (oi == optionIndex) o.copy(
                        optionName      = newName,
                        optionNameError = if (newName.trim().isNotEmpty()) null else o.optionNameError
                    ) else o
                }
                g.copy(options = updatedOpts)
            }
            s.copy(groups = updated)
        }
    }

    /** Updates extraPriceText; clears optionPriceError when value is non-negative (Req 8.8). */
    fun updateOptionExtraPrice(groupIndex: Int, optionIndex: Int, newText: String) {
        val sanitized = sanitizePriceInput(newText)
        _uiState.update { s ->
            val updated = s.groups.mapIndexed { gi, g ->
                if (gi != groupIndex) return@mapIndexed g
                val updatedOpts = g.options.mapIndexed { oi, o ->
                    if (oi == optionIndex) {
                        val parsed = sanitized.toDoubleOrNull() ?: 0.0
                        o.copy(
                            extraPriceText   = sanitized,
                            optionPriceError = if (parsed >= 0.0) null else o.optionPriceError
                        )
                    } else o
                }
                g.copy(options = updatedOpts)
            }
            s.copy(groups = updated)
        }
    }

    // ── Save & dismiss ────────────────────────────────────────────────────────

    /**
     * Loads an existing product into the form for editing.
     * Immediately populates all editable fields and sets isEditMode = true.
     * Then launches a coroutine to load customization groups/options from the DB.
     *
     * Requirements: 2.3, 2.4
     */
    fun loadForEdit(product: Product) {
        // Step 1 — synchronous state update with all basic fields
        val matchingCategory = _uiState.value.categories.firstOrNull { it.id == product.categoryId }
        _uiState.update { s ->
            s.copy(
                isEditMode        = true,
                editProductId     = product.id,
                emoji             = product.emoji,
                name              = product.name,
                description       = product.description,
                priceText         = product.basePrice.toString(),
                selectedCategory  = matchingCategory,
                isSaving          = false,
                saveResult        = null,
                nameError         = null,
                categoryError     = null,
                newCategoryNameError = null
            )
        }

        // Step 2 — async load of customization groups and options
        viewModelScope.launch {
            runCatching {
                val groups = database.customizationGroupDao().getGroupsByProductOnce(product.id)
                val groupDrafts = groups.map { group ->
                    val options = database.customizationOptionDao().getOptionsByGroupOnce(group.id)
                    GroupDraft(
                        draftId       = group.id,
                        groupName     = group.groupName,
                        selectionType = SelectionType.fromValue(group.selectionType)
                            ?: SelectionType.MULTIPLE_CHECKBOXES,
                        options       = options.map { opt ->
                            OptionDraft(
                                draftId        = opt.id,
                                optionName     = opt.optionName,
                                extraPriceText = if (opt.extraPrice == 0.0) "" else opt.extraPrice.toString()
                            )
                        }
                    )
                }
                _uiState.update { it.copy(groups = groupDrafts) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Error al cargar grupos: ${e.message}") }
            }
        }
    }

    /**
     * Validates the full form and, if valid, persists the complete product tree
     * inside a single Room transaction (Req 9).
     */
    fun save() {
        val s = _uiState.value
        if (s.isSaving) return

        // ── Field-level validation ────────────────────────────────────────────

        // Req 3.5: name must be non-blank
        val nameError = if (s.name.isBlank()) "El nombre es obligatorio" else null

        // Req 5.10: a category must be selected
        val categoryError = if (s.selectedCategory == null) "Selecciona una categoría" else null

        // Req 6.6: each group must have a non-blank name
        val groupErrors = s.groups.map { g ->
            if (g.groupName.trim().isBlank()) "El nombre del grupo es obligatorio" else null
        }
        val groupsWithErrors = s.groups.mapIndexed { i, g ->
            g.copy(groupNameError = groupErrors[i])
        }

        // Req 8.5, 8.6: each option must have a non-blank name and a non-negative price
        val groupsWithOptionErrors = groupsWithErrors.map { g ->
            val updatedOpts = g.options.map { o ->
                val nameErr  = if (o.optionName.trim().isBlank()) "El nombre de la opción es obligatorio" else null
                val priceErr = if (o.extraPrice < 0.0) "El precio debe ser ≥ 0" else null
                o.copy(optionNameError = nameErr, optionPriceError = priceErr)
            }
            g.copy(options = updatedOpts)
        }

        val hasErrors = nameError != null || categoryError != null ||
            groupErrors.any { it != null } ||
            groupsWithOptionErrors.any { g ->
                g.options.any { it.optionNameError != null || it.optionPriceError != null }
            }

        // Req 9.2: update all error fields simultaneously; do NOT start the transaction
        if (hasErrors) {
            _uiState.update { it.copy(
                nameError     = nameError,
                categoryError = categoryError,
                groups        = groupsWithOptionErrors
            ) }
            return
        }

        // No validation errors — start the save transaction (Req 9.2)
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runCatching {
                database.withTransaction {
                    val basePrice = s.priceText.toDoubleOrNull() ?: 0.0

                    if (s.isEditMode && s.editProductId != null) {
                        // ── Edit path: upsert with same ID, delete old groups first ──────
                        val editProductId = s.editProductId

                        // Delete all pre-existing groups (CASCADE removes their options)
                        val existingGroups = database.customizationGroupDao()
                            .getGroupsByProductOnce(editProductId)
                        existingGroups.forEach { group ->
                            database.customizationGroupDao().deleteById(group.id)
                        }

                        // Insert product with the SAME id (REPLACE upserts the ProductEntity)
                        productRepository.insert(
                            Product(
                                id          = editProductId,
                                emoji       = s.emoji,
                                name        = s.name.trim(),
                                description = s.description.trim(),
                                basePrice   = basePrice,
                                isActive    = true,
                                categoryId  = s.selectedCategory!!.id
                            )
                        )

                        // Insert new groups and options with fresh UUIDs
                        s.groups.forEach { group ->
                            val groupId = UUID.randomUUID().toString()
                            productRepository.insertGroup(
                                CustomizationGroupEntity(
                                    id            = groupId,
                                    productId     = editProductId,
                                    groupName     = group.groupName.trim(),
                                    selectionType = group.selectionType.value
                                )
                            )
                            group.options.forEach { option ->
                                productRepository.insertOption(
                                    CustomizationOptionEntity(
                                        id         = UUID.randomUUID().toString(),
                                        groupId    = groupId,
                                        optionName = option.optionName.trim(),
                                        extraPrice = option.extraPrice
                                    )
                                )
                            }
                        }
                    } else {
                        // ── Create path: generate a new UUID for productId ────────────────
                        val productId = UUID.randomUUID().toString()

                        // Req 9.3: insert the ProductEntity via ProductRepository.insert
                        productRepository.insert(
                            Product(
                                id          = productId,
                                emoji       = s.emoji,
                                name        = s.name.trim(),
                                description = s.description.trim(),
                                basePrice   = basePrice,
                                isActive    = true,
                                categoryId  = s.selectedCategory!!.id
                            )
                        )

                        // Req 9.3: for each GroupDraft insert its CustomizationGroupEntity + options
                        s.groups.forEach { group ->
                            val groupId = UUID.randomUUID().toString()
                            productRepository.insertGroup(
                                CustomizationGroupEntity(
                                    id            = groupId,
                                    productId     = productId,
                                    groupName     = group.groupName.trim(),
                                    selectionType = group.selectionType.value
                                )
                            )
                            group.options.forEach { option ->
                                productRepository.insertOption(
                                    CustomizationOptionEntity(
                                        id         = UUID.randomUUID().toString(),
                                        groupId    = groupId,
                                        optionName = option.optionName.trim(),
                                        extraPrice = option.extraPrice
                                    )
                                )
                            }
                        }
                    }
                }
                // Req 9.4: transaction succeeded
                _uiState.update { it.copy(isSaving = false, saveResult = SaveResult.Success) }
            }.onFailure { e ->
                // Req 9.6: Room auto-rolls back; surface the error without dismissing
                _uiState.update { it.copy(
                    isSaving = false,
                    error    = "Error al guardar: ${e.message}"
                ) }
            }
        }
    }

    /**
     * Resets all form state to the Initial Empty State.
     * Called on successful save dismissal and on "Cancelar" (Req 1.3, 9.8).
     */
    fun dismiss() {
        _uiState.update { current ->
            NewProductUiState(
                menus        = current.menus,
                selectedMenu = current.selectedMenu,
                categories   = current.categories
            )
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Sanitizes a string for the Decimal price field:
     * – Keeps only digits and the first `.`
     * – Truncates to at most two decimal places
     */
    private fun sanitizePriceInput(input: String): String {
        val clean = input.filter { it.isDigit() || it == '.' }
        val dotIndex = clean.indexOf('.')
        return if (dotIndex == -1) clean
        else {
            val decimals = clean.substring(dotIndex + 1).take(2)
            clean.substring(0, dotIndex + 1) + decimals
        }
    }

    private fun loadCategories(menuId: String) {
        categoryRepository.getCategoriesByMenu(menuId)
            .onEach { cats -> _uiState.update { it.copy(categories = cats) } }
            .launchIn(viewModelScope)
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    /**
     * Forces [isSaving] to the given value. For testing only.
     * Allows preservation tests to verify isSaving-related UI behavior without
     * triggering a full save transaction.
     */
    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    fun forceIsSavingForTest(isSaving: Boolean) {
        _uiState.update { it.copy(isSaving = isSaving) }
    }

    /**
     * Forces [saveResult] to the given value. For testing only.
     * Allows preservation tests to verify LaunchedEffect dismissal behavior.
     */
    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    fun forceSaveResultForTest(result: SaveResult?) {
        _uiState.update { it.copy(saveResult = result) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val productRepository: ProductRepository,
        private val categoryRepository: CategoryRepository,
        private val menuRepository: MenuRepository,
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NewProductViewModel(
                productRepository, categoryRepository, menuRepository, database
            ) as T
    }
}
