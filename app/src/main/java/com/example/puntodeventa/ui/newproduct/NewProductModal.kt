package com.example.puntodeventa.ui.newproduct

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.ui.theme.ButtonCancel
import com.example.puntodeventa.ui.theme.ButtonConfirm
import com.example.puntodeventa.ui.theme.InputBackground
import com.example.puntodeventa.ui.theme.InputBorder
import com.example.puntodeventa.ui.theme.InputHint
import com.example.puntodeventa.ui.theme.InputText
import androidx.compose.foundation.shape.RoundedCornerShape

// ── Field colors for modal-level fields (white background, green border) ──────

@Composable
private fun modalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = InputBorder,
    unfocusedBorderColor    = InputBorder,
    cursorColor             = InputText,
    focusedLabelColor       = InputBorder,
    unfocusedLabelColor     = InputHint,
    focusedTextColor        = InputText,
    unfocusedTextColor      = InputText,
    focusedContainerColor   = InputBackground,
    unfocusedContainerColor = InputBackground,
    errorBorderColor        = ButtonCancel,
    errorLabelColor         = ButtonCancel
)

// ── NewProductModal ───────────────────────────────────────────────────────────

/**
 * A [ModalBottomSheet] that presents the full "Nuevo Producto" form.
 *
 * Outer composable: collects [uiState] from [viewModel] internally and holds [sheetState]
 * and [scrollState] so they are stable with respect to field-level state changes. Any
 * field-level recomposition is scoped to [NewProductFormContent] and does NOT re-enter
 * the [ModalBottomSheet] container.
 *
 * All form state is owned by [viewModel]; this composable is stateless and routes
 * every user interaction back to the ViewModel. The sheet auto-dismisses when
 * [NewProductUiState.saveResult] transitions to [SaveResult.Success].
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 4.6, 4.7,
 *               5.2, 5.3, 5.9, 9.1, 9.5, 9.6, 9.7, 9.8
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductModal(
    viewModel: NewProductViewModel,
    onDismiss: () -> Unit
) {
    // Collect uiState internally so the call site does not need to hold a reference to the
    // changing state. This prevents the caller's composable scope from recomposing on every
    // field change, isolating all recompositions to NewProductFormContent (Req 2.1, 2.2).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-dismiss when save succeeds (Req 9.5)
    // Coarse-grained — legitimately belongs in the outer composable.
    LaunchedEffect(uiState.saveResult) {
        if (uiState.saveResult is SaveResult.Success) onDismiss()
    }

    // Hoist sheetState here so it is stable with respect to uiState changes (Req 2.1, 2.2).
    // Suppress swipe-dismiss while saving (Req 1.4) — coarse-grained sheet-level concern.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newVal ->
            if (uiState.isSaving) newVal != SheetValue.Hidden else true
        }
    )

    // Hoist scrollState here so scroll position survives field-level recompositions (Req 3.4).
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = {
            if (!uiState.isSaving) onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart    = 16.dp,
            topEnd      = 16.dp,
            bottomStart = 0.dp,
            bottomEnd   = 0.dp
        ),
        containerColor = InputBackground,
        dragHandle = null   // custom header replaces the default handle (Req 1.2)
    ) {
        // Delegate all field-level content to the inner composable.
        // Recompositions triggered by uiState field changes are scoped here and do NOT
        // propagate back up to ModalBottomSheet.
        NewProductFormContent(
            uiState     = uiState,
            viewModel   = viewModel,
            scrollState = scrollState,
            onDismiss   = onDismiss
        )
    }
}

/**
 * Inner composable that contains all form fields for the "Nuevo Producto" sheet.
 *
 * Receives [scrollState] from the outer [NewProductModal] so that scroll position is
 * preserved across recompositions caused by field changes. Recompositions of this
 * composable do NOT re-enter the [ModalBottomSheet] container in the outer composable.
 *
 * Requirements: 2.1, 2.2, 3.1–3.4
 */
@Composable
private fun NewProductFormContent(
    uiState: NewProductUiState,
    viewModel: NewProductViewModel,
    scrollState: ScrollState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)   // scrollable content (Req 1.6, 3.4)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // ── Header row: title + close button ─────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Nuevo Producto",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = InputText
            )
            // "X" button — disabled while saving (Req 1.4)
            IconButton(
                onClick  = { if (!uiState.isSaving) onDismiss() },
                enabled  = !uiState.isSaving
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint               = if (uiState.isSaving) InputHint else InputText
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Emoji picker (Req 2.1) ───────────────────────────────────────
        EmojiPickerButton(
            emoji           = uiState.emoji,
            expanded        = uiState.emojiPickerExpanded,
            onToggle        = { viewModel.toggleEmojiPicker() },
            onEmojiSelected = { viewModel.updateEmoji(it) }
        )

        Spacer(Modifier.height(12.dp))

        // ── Name field (Req 3.1) ─────────────────────────────────────────
        OutlinedTextField(
            value          = uiState.name,
            onValueChange  = { viewModel.updateName(it) },
            label          = { Text("Nombre") },
            singleLine     = true,
            isError        = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            colors         = modalFieldColors(),
            modifier       = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // ── Description field (Req 3.2) ──────────────────────────────────
        OutlinedTextField(
            value         = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label         = { Text("Descripción") },
            minLines      = 2,
            colors        = modalFieldColors(),
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // ── Price field (Req 3.3) ────────────────────────────────────────
        OutlinedTextField(
            value           = uiState.priceText,
            onValueChange   = { viewModel.updatePriceText(it) },
            label           = { Text("Precio") },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors          = modalFieldColors(),
            modifier        = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // ── Menu dropdown (Req 4.6, 4.7) ────────────────────────────────
        MenuDropdown(
            menus          = uiState.menus,
            selectedMenu   = uiState.selectedMenu,
            onMenuSelected = { viewModel.selectMenu(it) }
        )

        Spacer(Modifier.height(8.dp))

        // ── Category dropdown with inline creation (Req 5.2, 5.3, 5.9) ──
        CategoryDropdown(
            categories              = uiState.categories,
            selectedCategory        = uiState.selectedCategory,
            menuSelected            = uiState.selectedMenu != null,
            categoryError           = uiState.categoryError,
            showInlineCreation      = uiState.showInlineCategoryCreation,
            newCategoryName         = uiState.newCategoryName,
            newCategoryNameError    = uiState.newCategoryNameError,
            onCategorySelected      = { viewModel.selectCategory(it) },
            onStartInlineCreation   = { viewModel.startInlineCategoryCreation() },
            onInlineNameChange      = { viewModel.updateInlineCategoryName(it) },
            onSaveInlineCategory    = { viewModel.submitNewCategory() },
            onCancelInlineCategory  = { viewModel.cancelNewCategory() }
        )

        Spacer(Modifier.height(16.dp))

        // ── "Personalizaciones" section header (Req 6.1) ─────────────────
        Text(
            text       = "Personalizaciones",
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            color      = InputText
        )
        Spacer(Modifier.height(8.dp))

        // ── Group cards, keyed by stable draftId (Req 10.2) ─────────────
        uiState.groups.forEachIndexed { index, group ->
            androidx.compose.runtime.key(group.draftId) {
                GroupCard(
                    group                 = group,
                    groupIndex            = index,
                    onGroupNameChange     = { viewModel.updateGroupName(index, it) },
                    onSelectionTypeChange = { viewModel.updateGroupSelectionType(index, it) },
                    onRemoveGroup         = { viewModel.removeGroup(index) },
                    onAddOption           = { viewModel.addOption(index) },
                    onOptionNameChange    = { optIdx, name -> viewModel.updateOptionName(index, optIdx, name) },
                    onOptionPriceChange   = { optIdx, price -> viewModel.updateOptionExtraPrice(index, optIdx, price) },
                    onRemoveOption        = { optIdx -> viewModel.removeOption(index, optIdx) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── "+ Grupo" button (Req 6.1) ───────────────────────────────────
        OutlinedButton(
            onClick  = { viewModel.addGroup() },
            border   = BorderStroke(1.dp, InputBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Grupo", color = InputBorder)
        }

        Spacer(Modifier.height(16.dp))

        // ── Global error text (Req 9.6) ──────────────────────────────────
        if (uiState.error != null) {
            Text(
                text     = uiState.error,
                color    = ButtonCancel,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Bottom action row: "Cancelar" + "Crear producto" ─────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancelar — resets state and dismisses (Req 9.8)
            OutlinedButton(
                onClick  = { onDismiss() },
                enabled  = !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            // Crear producto — shows spinner while saving (Req 9.7)
            Button(
                onClick  = { viewModel.save() },
                enabled  = !uiState.isSaving,
                colors   = ButtonDefaults.buttonColors(containerColor = ButtonConfirm),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Color.White
                    )
                } else {
                    Text("Crear producto")
                }
            }
        }

        // Extra bottom padding so the action row doesn't sit flush against
        // the system navigation bar on edge-to-edge devices
        Spacer(Modifier.height(16.dp))
    }
}

// ── Menu dropdown ─────────────────────────────────────────────────────────────

/**
 * An [ExposedDropdownMenuBox] that lists all available [MenuItem]s.
 *
 * When [menus] is empty the field is disabled and shows the placeholder text
 * "Sin menús disponibles" (Req 4.6, 4.7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuDropdown(
    menus: List<MenuItem>,
    selectedMenu: MenuItem?,
    onMenuSelected: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val isEmpty = menus.isEmpty()

    ExposedDropdownMenuBox(
        expanded         = if (isEmpty) false else expanded,
        onExpandedChange = { if (!isEmpty) expanded = !expanded },
        modifier         = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value         = if (isEmpty) "Sin menús disponibles" else selectedMenu?.name ?: "",
            onValueChange = {},
            readOnly      = true,
            enabled       = !isEmpty,
            label         = { Text("Menú") },
            trailingIcon  = {
                if (!isEmpty) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors        = modalFieldColors(),
            modifier      = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        if (!isEmpty) {
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                menus.forEach { menu ->
                    DropdownMenuItem(
                        text    = { Text(menu.name) },
                        onClick = {
                            onMenuSelected(menu)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ── Category dropdown with inline creation ────────────────────────────────────

/**
 * An [ExposedDropdownMenuBox] that lists all [Category]s for the selected menu, with
 * a special "+ Nueva categoría..." entry at the bottom rendered in [ButtonConfirm] color.
 *
 * When [menuSelected] is false the dropdown is disabled (Req 5.9).
 * [showInlineCreation] toggles an inline creation sub-form (Req 5.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategory: Category?,
    menuSelected: Boolean,
    categoryError: String?,
    showInlineCreation: Boolean,
    newCategoryName: String,
    newCategoryNameError: String?,
    onCategorySelected: (Category) -> Unit,
    onStartInlineCreation: () -> Unit,
    onInlineNameChange: (String) -> Unit,
    onSaveInlineCategory: () -> Unit,
    onCancelInlineCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded         = if (!menuSelected) false else expanded,
            onExpandedChange = { if (menuSelected) expanded = !expanded },
            modifier         = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value          = selectedCategory?.name ?: "",
                onValueChange  = {},
                readOnly       = true,
                enabled        = menuSelected,
                label          = { Text("Categoría") },
                isError        = categoryError != null,
                supportingText = categoryError?.let { { Text(it) } },
                trailingIcon   = {
                    if (menuSelected) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors         = modalFieldColors(),
                modifier       = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            if (menuSelected) {
                ExposedDropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Existing categories
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text    = { Text(category.name) },
                            onClick = {
                                onCategorySelected(category)
                                expanded = false
                            }
                        )
                    }

                    // Special entry: "+ Nueva categoría..." in ButtonConfirm color (Req 5.2)
                    DropdownMenuItem(
                        text    = {
                            Text(
                                text  = "+ Nueva categoría...",
                                color = ButtonConfirm
                            )
                        },
                        onClick = {
                            onStartInlineCreation()
                            expanded = false
                        }
                    )
                }
            }
        }

        // ── Inline category creation form (Req 5.3) ──────────────────────────
        if (showInlineCreation) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value          = newCategoryName,
                onValueChange  = { onInlineNameChange(it) },
                label          = { Text("Nombre de la nueva categoría") },
                singleLine     = true,
                isError        = newCategoryNameError != null,
                supportingText = newCategoryNameError?.let { { Text(it) } },
                colors         = modalFieldColors(),
                modifier       = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Button(
                    onClick  = onSaveInlineCategory,
                    colors   = ButtonDefaults.buttonColors(containerColor = ButtonConfirm),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar categoría", color = Color.White)
                }

                TextButton(
                    onClick  = onCancelInlineCategory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = InputBorder)
                }
            }
        }
    }
}
