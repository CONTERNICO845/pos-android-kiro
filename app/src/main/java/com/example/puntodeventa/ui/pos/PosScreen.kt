package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntodeventa.data.local.CustomizationGroupDao
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionDao
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.data.model.Product

/**
 * Main POS screen composable containing the two-panel layout:
 * CatalogPanel (70% width) + CartPanel (30% width).
 *
 * This composable is placed as a sibling NEXT to the AppNavRail in the parent Row
 * (in MainActivity), so it does NOT include its own navigation rail.
 *
 * Modal state for the product detail dialog is managed locally. When a product
 * is tapped, customization groups and options are loaded from the provided DAOs
 * via a LaunchedEffect, and the ProductModal is displayed.
 *
 * Satisfies Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.6, 3.1, 6.5, 6.6
 */
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    customizationGroupDao: CustomizationGroupDao,
    customizationOptionDao: CustomizationOptionDao,
    menuItems: List<MenuItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editingCartItem by viewModel.editingCartItem.collectAsStateWithLifecycle()

    // ── Local modal state ─────────────────────────────────────────────────────
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var customizationGroups by remember { mutableStateOf<List<CustomizationGroupEntity>>(emptyList()) }
    var customizationOptions by remember { mutableStateOf<Map<String, List<CustomizationOptionEntity>>>(emptyMap()) }

    // ── Edit mode: resolve the product for the editing cart item ──────────────
    val editingProduct = remember(editingCartItem, uiState.products) {
        val item = editingCartItem
        if (item != null) {
            uiState.products.find { it.id == item.productId }
        } else null
    }
    var editCustomizationGroups by remember { mutableStateOf<List<CustomizationGroupEntity>>(emptyList()) }
    var editCustomizationOptions by remember { mutableStateOf<Map<String, List<CustomizationOptionEntity>>>(emptyMap()) }

    // ── Load customization data when editing a cart item ──────────────────────
    LaunchedEffect(editingProduct) {
        val product = editingProduct
        if (product != null) {
            val groups = customizationGroupDao.getGroupsByProductOnce(product.id)
            val optionsMap = groups.associate { group ->
                group.id to customizationOptionDao.getOptionsByGroupOnce(group.id)
            }
            editCustomizationGroups = groups
            editCustomizationOptions = optionsMap
        } else {
            editCustomizationGroups = emptyList()
            editCustomizationOptions = emptyMap()
        }
    }

    // ── Load customization data when a product is selected ────────────────────
    LaunchedEffect(selectedProduct) {
        val product = selectedProduct
        if (product != null) {
            val groups = customizationGroupDao.getGroupsByProductOnce(product.id)
            val optionsMap = groups.associate { group ->
                group.id to customizationOptionDao.getOptionsByGroupOnce(group.id)
            }
            customizationGroups = groups
            customizationOptions = optionsMap
        } else {
            customizationGroups = emptyList()
            customizationOptions = emptyMap()
        }
    }

    // ── Snackbar for error display ────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val errorMessage = uiState.error
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isCheckoutVisible) {
                CheckoutPanel(
                    checkoutState = uiState.checkoutState,
                    cartTotal = uiState.cartTotal,
                    isCompletarEnabled = viewModel.isCompletarOrdenEnabled(),
                    onCustomerNameChange = { viewModel.updateCustomerName(it) },
                    onPaymentStatusSelected = { viewModel.selectPaymentStatus(it) },
                    onDenominationPressed = { viewModel.addDenomination(it) },
                    onClearCashReceived = { viewModel.clearCashReceived() },
                    onCompletarOrden = { viewModel.showConfirmationModal() },
                    onCancelar = { viewModel.hideCheckout() },
                    onBack = { viewModel.hideCheckout() },
                    modifier = Modifier.weight(0.7f)
                )
            } else {
                CatalogPanel(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    products = uiState.products,
                    menuItems = menuItems,
                    selectedMenuId = uiState.selectedMenuId,
                    searchQuery = uiState.searchQuery,
                    isSearchVisible = uiState.isSearchVisible,
                    onCategorySelected = { category -> viewModel.selectCategory(category) },
                    onProductTapped = { product -> selectedProduct = product },
                    onMenuSelected = { menuId -> viewModel.selectMenu(menuId) },
                    onSearchClick = { viewModel.toggleSearch() },
                    onDividerClick = { viewModel.toggleDivider() },
                    onQueryChange = { query -> viewModel.updateSearchQuery(query) },
                    onClearSearch = { viewModel.clearSearch() },
                    modifier = Modifier.weight(0.7f)
                )
            }

            CartPanel(
                cartItems = uiState.cartItems,
                cartTotal = uiState.cartTotal,
                onRemoveItem = { cartItemId -> viewModel.removeFromCart(cartItemId) },
                onItemClick = { viewModel.startEditingItem(it.id) },
                onCompleteOrder = { viewModel.showCheckout() },
                isCartEmpty = uiState.cartItems.isEmpty(),
                modifier = Modifier.weight(0.3f)
            )
        }
    }

    // ── Confirmation Modal ────────────────────────────────────────────────────
    if (uiState.isConfirmationModalVisible) {
        ConfirmationModal(
            total = uiState.cartTotal,
            paymentStatus = uiState.checkoutState.paymentStatus,
            cashReceived = uiState.checkoutState.cashReceived,
            change = uiState.checkoutState.cashReceived - uiState.cartTotal,
            buttonText = uiState.confirmButtonText,
            isButtonEnabled = !uiState.checkoutState.isPrinting,
            onConfirm = { viewModel.confirmPayment() },
            onDismiss = { viewModel.dismissConfirmationModal() }
        )
    }

    // ── Product Modal (add mode) ─────────────────────────────────────────────
    if (selectedProduct != null) {
        ProductModal(
            product = selectedProduct!!,
            customizationGroups = customizationGroups,
            customizationOptions = customizationOptions,
            onAddToCart = { cartItem ->
                viewModel.addToCart(cartItem)
                selectedProduct = null
            },
            onDismiss = { selectedProduct = null }
        )
    }

    // ── Product Modal (edit mode) ─────────────────────────────────────────────
    if (editingCartItem != null && editingProduct != null) {
        ProductModal(
            product = editingProduct!!,
            customizationGroups = editCustomizationGroups,
            customizationOptions = editCustomizationOptions,
            onAddToCart = { updatedItem ->
                viewModel.updateCartItem(updatedItem)
            },
            onDismiss = {
                // Cancel editing: reset editingCartItem without modifying the cart
                viewModel.cancelEditing()
            },
            editingCartItem = editingCartItem
        )
    }
}
