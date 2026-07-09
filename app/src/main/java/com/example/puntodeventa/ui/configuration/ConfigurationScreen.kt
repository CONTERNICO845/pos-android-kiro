package com.example.puntodeventa.ui.configuration

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.ui.newproduct.NewProductModal
import com.example.puntodeventa.ui.newproduct.NewProductViewModel
import com.example.puntodeventa.ui.theme.CardBackground
import com.example.puntodeventa.ui.theme.CardText
import com.example.puntodeventa.ui.theme.InputBackground
import com.example.puntodeventa.ui.theme.InputBorder
import com.example.puntodeventa.ui.theme.InputHint
import com.example.puntodeventa.ui.theme.InputText
import com.example.puntodeventa.ui.theme.NavRailIconSelected

@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel,
    newProductViewModel: NewProductViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showModal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Row 1 — Category tabs
        CategoryTabsRow(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )

        // Row 2 — Search + action buttons
        ActionBarRow(
            searchQuery = uiState.searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onNuevoProductoClick = { showModal = true },
            onModificarJsonClick = { Log.d("ConfigurationScreen", "Modificar JSON") },
            onImportarJsonClick = { Log.d("ConfigurationScreen", "Importar JSON") },
            onExportarJsonClick = { Log.d("ConfigurationScreen", "Exportar JSON") }
        )

        // Product list area — state machine as per design
        Box(modifier = Modifier.fillMaxSize()) {
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
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
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
                        color = if (isSelected) NavRailIconSelected else CardText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }

    if (categories.size > 4) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = CardBackground,
            contentColor = CardText,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = CardBackground   // suppress pill by matching container color
                )
            },
            modifier = modifier.fillMaxWidth()
        ) {
            tabs()
        }
    } else {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = CardBackground,
            contentColor = CardText,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = CardBackground   // suppress pill by matching container color
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search field — weight(1f) so it fills remaining horizontal space
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { if (it.length <= 100) onQueryChange(it) },
            label = { Text("Buscar Producto") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InputBorder,
                unfocusedBorderColor = InputBorder,
                cursorColor = InputText,
                focusedLabelColor = InputBorder,
                unfocusedLabelColor = InputHint,
                focusedTextColor = InputText,
                unfocusedTextColor = InputText,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground
            ),
            modifier = Modifier.weight(1f)
        )

        // Outlined button: Modificar JSON
        OutlinedButton(
            onClick = onModificarJsonClick,
            border = BorderStroke(1.dp, NavRailIconSelected),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Modificar JSON",
                color = NavRailIconSelected,
                fontSize = 12.sp
            )
        }

        // Outlined button: Importar JSON
        OutlinedButton(
            onClick = onImportarJsonClick,
            border = BorderStroke(1.dp, NavRailIconSelected),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Importar JSON",
                color = NavRailIconSelected,
                fontSize = 12.sp
            )
        }

        // Outlined button: Exportar JSON
        OutlinedButton(
            onClick = onExportarJsonClick,
            border = BorderStroke(1.dp, NavRailIconSelected),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "Exportar JSON",
                color = NavRailIconSelected,
                fontSize = 12.sp
            )
        }

        // Filled button: Nuevo Producto
        Button(
            onClick = onNuevoProductoClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NavRailIconSelected,
                contentColor = CardText
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
