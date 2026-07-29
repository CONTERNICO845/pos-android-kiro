package com.example.puntodeventa.ui.configuration

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.ui.newproduct.NewProductModal
import com.example.puntodeventa.ui.newproduct.NewProductViewModel
import androidx.compose.material3.MaterialTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel,
    newProductViewModel: NewProductViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showModal by remember { mutableStateOf(false) }

    // ── SAF Launchers ─────────────────────────────────────────────────────────

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.onExportUriReceived(uri, context.contentResolver)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCatalogFromUri(uri, context.contentResolver)
        }
    }

    // ── Toast feedback ────────────────────────────────────────────────────────

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Row 1 — Category tabs + delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryTabsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                modifier = Modifier.weight(1f)
            )
            if (uiState.selectedCategory != null) {
                IconButton(onClick = { viewModel.requestDeleteCategory() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar categoría"
                    )
                }
            }
        }

        // Row 2 — Search + action buttons
        ActionBarRow(
            searchQuery = uiState.searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onNuevoProductoClick = { showModal = true },
            onModificarJsonClick = { viewModel.openJsonEditor() },
            onImportarJsonClick = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onExportarJsonClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                viewModel.exportCatalog()
                exportLauncher.launch("catalogo_$timestamp.json")
            },
            isExporting = uiState.isExporting,
            isImporting = uiState.isImporting
        )

        // Product list area — state machine as per design
        Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.categories.isEmpty() -> {
                    Text(
                        text = "No hay categorías disponibles",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.filteredProducts.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    Text(
                        text = "No se encontraron productos",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.filteredProducts.isEmpty() -> {
                    Text(
                        text = "No hay productos en esta categoría",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(uiState.filteredProducts, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isMenuExpanded = uiState.expandedProductMenuId == product.id,
                                onToggleActive = { viewModel.toggleProductActive(it) },
                                onMenuOpen = { viewModel.setExpandedProductMenu(product.id) },
                                onMenuDismiss = { viewModel.setExpandedProductMenu(null) },
                                onEditar = { product ->
                                    newProductViewModel.loadForEdit(product)
                                    showModal = true
                                    viewModel.setExpandedProductMenu(null)
                                },
                                onDuplicar = { viewModel.duplicateProduct(it) },
                                onEliminar = { viewModel.deleteProduct(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModal) {
        NewProductModal(
            viewModel = newProductViewModel,
            onDismiss = {
                newProductViewModel.dismiss()
                showModal = false
            }
        )
    }

    if (uiState.showDeleteCategoryDialog) {
        DeleteCategoryDialog(
            onConfirm = { viewModel.confirmDeleteCategory() },
            onDismiss = { viewModel.dismissDeleteCategoryDialog() }
        )
    }

    // ── Import Confirm Dialog ─────────────────────────────────────────────────

    if (uiState.showImportConfirmDialog) {
        ImportConfirmDialog(
            onConfirm = {
                viewModel.confirmImport()
                if (uiState.showJsonEditor) {
                    viewModel.closeJsonEditor()
                }
            },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    // ── JSON Editor Dialog ────────────────────────────────────────────────────

    if (uiState.showJsonEditor) {
        JsonEditorDialog(
            content = uiState.jsonEditorContent,
            error = uiState.jsonEditorError,
            onApply = { editedText -> viewModel.applyJsonEditorChanges(editedText) },
            onDismiss = { viewModel.closeJsonEditor() }
        )
    }
}

/**
 * Displays the category tabs row at the top of the configuration screen.
 *
 * Uses [ScrollableTabRow] when there are more than 4 categories, [TabRow] otherwise.
 * The tab indicator is suppressed by matching the container color (AC-02.1, AC-02.6).
 * Selected tab text uses [NavRailIconSelected] bold; unselected uses [CardText] normal weight.
 */
@Composable
private fun CategoryTabsRow(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = if (selectedCategory == null) {
        0
    } else {
        categories.indexOfFirst { it.id == selectedCategory.id }.coerceAtLeast(0)
    }

    // Shared tab content builder
    val tabs: @Composable () -> Unit = {
        categories.forEachIndexed { index, category ->
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = category.name,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }

    if (categories.size > 4) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.primaryContainer   // suppress pill by matching container color
                )
            },
            modifier = modifier.fillMaxWidth()
        ) {
            tabs()
        }
    } else {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primaryContainer   // suppress pill by matching container color
                    )
                }
            },
            modifier = modifier.fillMaxWidth()
        ) {
            tabs()
        }
    }
}

/**
 * Displays the action bar row: search field + JSON action buttons + "Nuevo Producto" button.
 *
 * The search field is limited to 100 characters (AC-04.1, AC-04.2).
 * The three outlined JSON buttons use [NavRailIconSelected] border and text (AC-03.1, AC-03.3).
 * The filled "Nuevo Producto" button uses [NavRailIconSelected] background with [CardText] label (AC-03.5).
 */
@Composable
private fun ActionBarRow(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onNuevoProductoClick: () -> Unit,
    onModificarJsonClick: () -> Unit,
    onImportarJsonClick: () -> Unit,
    onExportarJsonClick: () -> Unit,
    isExporting: Boolean = false,
    isImporting: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
            .testTag("ActionBarRow"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search field — weight(1f) so it fills remaining horizontal space
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { if (it.length <= 100) onQueryChange(it) },
            label = { Text("Buscar Producto") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.weight(1f)
        )

        // Outlined button: Modificar JSON
        OutlinedButton(
            onClick = onModificarJsonClick,
            enabled = !isExporting && !isImporting,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Modificar JSON",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }

        // Outlined button: Importar JSON
        OutlinedButton(
            onClick = onImportarJsonClick,
            enabled = !isExporting && !isImporting,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Importar JSON",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }

        // Outlined button: Exportar JSON
        OutlinedButton(
            onClick = onExportarJsonClick,
            enabled = !isExporting && !isImporting,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = if (isExporting) "Exportando..." else "Exportar JSON",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }

        // Filled button: Nuevo Producto
        Button(
            onClick = onNuevoProductoClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Nuevo Producto",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeleteCategoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar categoría") },
        text = { Text("¿Estás seguro? Eliminar esta categoría eliminará permanentemente todos los productos dentro de ella.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ImportConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar Catálogo") },
        text = {
            Text(
                "Esto reemplazará TODO el catálogo actual (categorías, productos y personalizaciones). " +
                "Las órdenes existentes no se verán afectadas. ¿Continuar?"
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Importar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun JsonEditorDialog(
    content: String,
    error: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember(content) { mutableStateOf(content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar Catálogo JSON") },
        text = {
            Column {
                Text(
                    text = "Edita el JSON del catálogo completo. Los cambios reemplazarán todo el catálogo actual.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(editedText) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
