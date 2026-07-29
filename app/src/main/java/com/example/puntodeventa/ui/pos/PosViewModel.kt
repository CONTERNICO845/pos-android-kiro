package com.example.puntodeventa.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.local.OrderItemCustomizationEntity
import com.example.puntodeventa.data.local.OrderItemEntity
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PosUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,   // null = "TODO" tab
    val selectedMenuId: String? = null,        // null = no menu filter
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val cartTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isModalOpen: Boolean = false,
    val selectedProduct: Product? = null,
    val isCheckoutVisible: Boolean = false,
    val checkoutState: CheckoutState = CheckoutState(),
    val isConfirmationModalVisible: Boolean = false,
    val confirmButtonText: String = "Confirmar Pago",
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PosViewModel(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val menuId: String,
    private val printerPreferencesRepository: PrinterPreferencesRepository? = null
) : ViewModel() {

    // ── Internal mutable state ────────────────────────────────────────────────

    private val _selectedMenu = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _isModalOpen = MutableStateFlow(false)
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    private val _checkoutState = MutableStateFlow(CheckoutState())
    private val _isCheckoutVisible = MutableStateFlow(false)
    private val _isConfirmationModalVisible = MutableStateFlow(false)
    private val _confirmButtonText = MutableStateFlow("Confirmar Pago")
    private val _editingCartItem = MutableStateFlow<CartItem?>(null)
    val editingCartItem: StateFlow<CartItem?> = _editingCartItem

    // ── Search state ──────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchVisible = MutableStateFlow(false)

    // ── Reactive pipeline ─────────────────────────────────────────────────────

    // Step 1 — Share categories Flow (single DB subscription)
    private val categoriesFlow = categoryRepository.getCategoriesByMenu(menuId)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1
        )

    init {
        // Auto-select "TODO" tab (null) on init. Mark loading as false once
        // categories are first emitted.
        categoriesFlow
            .onEach { _isLoading.value = false }
            .launchIn(viewModelScope)
    }

    // Step 2 — flatMapLatest: when selectedCategory or selectedMenu changes, load products.
    // _selectedMenu non-null → only categories whose associatedMenuId matches.
    // _selectedCategory null (TODO tab) → load ALL products from visible categories.
    // _selectedCategory specific → load products for that category only (if it passes menu filter).
    // Then apply debounced search filter by name (case-insensitive).
    private val debouncedSearchQuery = _searchQuery.debounce(300)

    private val productsFlow = combine(
        combine(_selectedCategory, _selectedMenu) { category, selectedMenu ->
            Pair(category, selectedMenu)
        }.flatMapLatest { (category, selectedMenu) ->
            if (category != null) {
                // Specific category selected — check if it passes the menu filter
                if (selectedMenu != null && category.associatedMenuId != selectedMenu) {
                    // Category doesn't belong to the selected menu → empty
                    flowOf(emptyList())
                } else {
                    productRepository.getActiveProductsByCategory(category.id)
                        .map { products -> products.sortedBy { it.name.lowercase() } }
                }
            } else {
                // "TODO" tab (null category) — all products from filtered categories
                categoriesFlow.flatMapLatest { cats ->
                    val filteredCats = if (selectedMenu != null) {
                        cats.filter { it.associatedMenuId == selectedMenu }
                    } else {
                        cats
                    }
                    if (filteredCats.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val productFlows = filteredCats.map { cat ->
                            productRepository.getActiveProductsByCategory(cat.id)
                        }
                        combine(productFlows) { arrays ->
                            arrays.toList().flatten().sortedBy { it.name.lowercase() }
                        }
                    }
                }
            }
        },
        debouncedSearchQuery
    ) { products, query ->
        if (query.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    // Step 3 — Derive cart total from cart items (excluding dividers)
    private val cartTotalFlow = _cartItems.map { items ->
        items.filter { !it.isDivider }.sumOf { it.totalPrice }
    }

    // Step 4 — Combine all streams into a single PosUiState.
    // Uses nested typed combines (max 5 flows each) to avoid the vararg
    // combine(Array<Any?>) overload which can silently drop emissions.
    private data class CatalogSlice(
        val categories: List<Category>,
        val selectedCategory: Category?,
        val selectedMenuId: String?,
        val products: List<Product>,
        val cartItems: List<CartItem>,
        val cartTotal: Double
    )

    private data class UiSlice(
        val isLoading: Boolean,
        val error: String?,
        val isModalOpen: Boolean,
        val selectedProduct: Product?,
        val isCheckoutVisible: Boolean,
        val searchQuery: String,
        val isSearchVisible: Boolean
    )

    private data class CheckoutSlice(
        val checkoutState: CheckoutState,
        val isConfirmationModalVisible: Boolean,
        val confirmButtonText: String
    )

    private val catalogSlice = combine(
        categoriesFlow,
        _selectedCategory,
        productsFlow,
        _cartItems,
        cartTotalFlow
    ) { categories, selectedCategory, products, cartItems, cartTotal ->
        CatalogSlice(categories, selectedCategory, null, products, cartItems, cartTotal)
    }.let { baseSlice ->
        combine(baseSlice, _selectedMenu) { base, selectedMenuId ->
            base.copy(selectedMenuId = selectedMenuId)
        }
    }

    private val uiSlice = combine(
        _isLoading,
        _error,
        _isModalOpen,
        _selectedProduct,
        _isCheckoutVisible
    ) { isLoading, error, isModalOpen, selectedProduct, isCheckoutVisible ->
        UiSlice(isLoading, error, isModalOpen, selectedProduct, isCheckoutVisible,
            searchQuery = "", isSearchVisible = false)
    }.let { baseSlice ->
        combine(baseSlice, _searchQuery, _isSearchVisible) { base, searchQuery, isSearchVisible ->
            base.copy(searchQuery = searchQuery, isSearchVisible = isSearchVisible)
        }
    }

    private val checkoutSlice = combine(
        _checkoutState,
        _isConfirmationModalVisible,
        _confirmButtonText
    ) { checkoutState, isConfirmationModalVisible, confirmButtonText ->
        CheckoutSlice(checkoutState, isConfirmationModalVisible, confirmButtonText)
    }

    val uiState: StateFlow<PosUiState> = combine(
        catalogSlice,
        uiSlice,
        checkoutSlice
    ) { catalog, ui, checkout ->
        PosUiState(
            categories = catalog.categories,
            selectedCategory = catalog.selectedCategory,
            selectedMenuId = catalog.selectedMenuId,
            products = catalog.products,
            cartItems = catalog.cartItems,
            cartTotal = catalog.cartTotal,
            isLoading = ui.isLoading,
            error = ui.error,
            isModalOpen = ui.isModalOpen,
            selectedProduct = ui.selectedProduct,
            isCheckoutVisible = ui.isCheckoutVisible,
            checkoutState = checkout.checkoutState,
            isConfirmationModalVisible = checkout.isConfirmationModalVisible,
            confirmButtonText = checkout.confirmButtonText,
            searchQuery = ui.searchQuery,
            isSearchVisible = ui.isSearchVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PosUiState(isLoading = true)
    )

    // ── Public write functions ────────────────────────────────────────────────

    /** Selects a category tab. Pass null for the "TODO" (all products) tab.
     *  Clears search query and hides search field when a category is selected (Req 2.6). */
    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
        // Req 2.6: When a category tab is selected, clear search and hide search field
        _searchQuery.value = ""
        _isSearchVisible.value = false
    }

    /**
     * Selects a menu filter. Pass null to deselect (show all products).
     * When a menu filter is applied and the currently selected category doesn't belong
     * to that menu, auto-resets _selectedCategory to null (Req 1.6).
     */
    fun selectMenu(menuId: String?) {
        _selectedMenu.value = menuId
        // Req 1.6: If menu filter is applied and current category doesn't belong, reset category
        val currentCategory = _selectedCategory.value
        if (menuId != null && currentCategory != null && currentCategory.associatedMenuId != menuId) {
            _selectedCategory.value = null
        }
    }

    /** Toggles the search field visibility. Clears query when hiding (Req 2.1, 2.3). */
    fun toggleSearch() {
        val wasVisible = _isSearchVisible.value
        _isSearchVisible.value = !wasVisible
        // Clear query when hiding the search field
        if (wasVisible) {
            _searchQuery.value = ""
        }
    }

    /** Updates the search query, limited to 100 characters (Req 2.2). */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.take(100)
    }

    /** Clears the search query (Req 2.5). */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /** Adds a cart item to the in-memory cart. Always creates a new line item. */
    fun addToCart(cartItem: CartItem) {
        _cartItems.value = _cartItems.value + cartItem
    }

    /** Removes a cart item from the in-memory cart by its id. */
    fun removeFromCart(cartItemId: String) {
        val updated = _cartItems.value.filter { it.id != cartItemId }
        _cartItems.value = updated
        // Auto-hide checkout if cart becomes empty
        if (updated.isEmpty() && _isCheckoutVisible.value) {
            _isCheckoutVisible.value = false
        }
    }

    /**
     * Finds the CartItem by ID in the current cart and sets it as the editing item.
     * If no item with the given ID exists, this is a no-op (editingCartItem stays null).
     */
    fun startEditingItem(cartItemId: String) {
        val item = _cartItems.value.find { it.id == cartItemId }
        if (item != null) {
            _editingCartItem.value = item
        }
    }

    /**
     * Cancels the current editing operation without modifying the cart.
     * Simply resets _editingCartItem to null so the modal closes.
     * (Req 5.4)
     */
    fun cancelEditing() {
        _editingCartItem.value = null
    }

    /**
     * Replaces an existing cart item in-place by matching the provided CartItem's id.
     * Preserves list order — the updated item stays at the same index.
     * If no item with the given id exists, the cart is left unchanged (no-op).
     * After a successful update, resets _editingCartItem to null.
     * Cart total is recalculated automatically via cartTotalFlow.
     */
    fun updateCartItem(cartItem: CartItem) {
        val currentItems = _cartItems.value
        val index = currentItems.indexOfFirst { it.id == cartItem.id }
        if (index >= 0) {
            _cartItems.value = currentItems.toMutableList().apply {
                set(index, cartItem)
            }
        }
        _editingCartItem.value = null
    }

    /**
     * Appends a divider item to the cart. The divider acts as a visual separator
     * and is excluded from all total calculations and order persistence.
     */
    fun addDivider() {
        val dividerItem = CartItem(
            id = UUID.randomUUID().toString(),
            productId = "",
            productName = "--- DIVISOR ---",
            emoji = "",
            basePrice = 0.00,
            quantity = 1,
            selectedCustomizations = emptyList(),
            extraNotes = "",
            totalPrice = 0.00,
            isDivider = true
        )
        _cartItems.value = _cartItems.value + dividerItem
    }

    /**
     * Smart scissors toggle logic (Req 2.1–2.6):
     * - If editing a divider → remove that specific divider by id, clear editing state
     * - If cart is empty → no-op
     * - If last item is a divider → remove it (toggle off)
     * - Otherwise → append a new divider
     */
    fun toggleDivider() {
        val editing = _editingCartItem.value
        if (editing != null && editing.isDivider) {
            _cartItems.value = _cartItems.value.filter { it.id != editing.id }
            _editingCartItem.value = null
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) return
        if (items.last().isDivider) {
            _cartItems.value = items.dropLast(1)
            return
        }
        _cartItems.value = items + CartItem(
            id = UUID.randomUUID().toString(),
            productId = "",
            productName = "--- DIVISOR ---",
            emoji = "",
            basePrice = 0.0,
            quantity = 1,
            selectedCustomizations = emptyList(),
            extraNotes = "",
            totalPrice = 0.0,
            isDivider = true
        )
    }

    /** Shows the checkout panel, only if the cart is non-empty. */
    fun showCheckout() {
        if (_cartItems.value.isNotEmpty()) {
            _isCheckoutVisible.value = true
            _checkoutState.value = CheckoutState() // fresh state each time
        }
    }

    /** Hides the checkout panel, returning to catalog. Does NOT modify cart. */
    fun hideCheckout() {
        _isCheckoutVisible.value = false
    }

    /** Updates the customer name, trimming whitespace and truncating to 40 chars. */
    fun updateCustomerName(name: String) {
        _checkoutState.value = _checkoutState.value.copy(
            customerName = name.trim().take(40)
        )
    }

    /** Selects the payment status. Exactly one is always active. */
    fun selectPaymentStatus(status: PaymentStatus) {
        _checkoutState.value = _checkoutState.value.copy(paymentStatus = status)
    }

    /** Adds a denomination to cash received. Ignores if it would exceed $999,999.99.
     *  Recalculates cashReceived as sum of (denomination × count) + custom amounts. */
    fun addDenomination(value: Int) {
        val current = _checkoutState.value
        val newCounts = current.denominationCounts.toMutableMap()
        newCounts[value] = (newCounts[value] ?: 0) + 1
        val newTotal = newCounts.entries.sumOf { (denom, count) -> denom.toLong() * count }.toDouble() +
            current.customAmounts.sum()
        if (newTotal > 999_999.99) return // ignore press if would exceed max
        _checkoutState.value = current.copy(
            denominationCounts = newCounts,
            cashReceived = newTotal
        )
    }

    /**
     * Parses the input amount and adds it to cashReceived.
     * Ignores if null, zero, or negative. Guards against exceeding $999,999.99.
     * Clears the input by design (UI layer should clear the text field on success).
     */
    fun addCustomAmount(amount: String) {
        val parsed = amount.toDoubleOrNull() ?: return
        if (parsed <= 0.0) return
        val current = _checkoutState.value
        val newTotal = current.cashReceived + parsed
        if (newTotal > 999_999.99) return // ignore if would exceed max
        _checkoutState.value = current.copy(
            cashReceived = newTotal,
            customAmounts = current.customAmounts + parsed
        )
    }

    /** Clears all denomination counts, custom amounts, and resets cash received to zero. */
    fun clearCashReceived() {
        _checkoutState.value = _checkoutState.value.copy(
            denominationCounts = emptyMap(),
            customAmounts = emptyList(),
            cashReceived = 0.0
        )
    }

    /** Returns whether the "Completar Orden" button should be enabled.
     *  Disabled when:
     *  - customer name is blank (trimmed empty)
     *  - payment status is PAGADO and cash received < cart total
     */
    fun isCompletarOrdenEnabled(): Boolean {
        val state = _checkoutState.value
        val cartTotal = _cartItems.value.filter { !it.isDivider }.sumOf { it.totalPrice }
        if (state.customerName.trim().isEmpty()) return false
        if (state.paymentStatus == PaymentStatus.PAGADO && state.cashReceived < cartTotal) return false
        return true
    }

    /** Shows the confirmation modal. Called when "Completar Orden" is pressed. */
    fun showConfirmationModal() {
        _isConfirmationModalVisible.value = true
    }

    /** Dismisses the confirmation modal. Called when "Cancelar" is pressed in the modal. */
    fun dismissConfirmationModal() {
        _isConfirmationModalVisible.value = false
    }

    /**
     * Initiates the payment confirmation flow:
     * 1. Guards against empty cart (Req 8.8)
     * 2. Disables buttons and changes text to "Imprimiendo Ticket"
     * 3. Validates printer IP
     * 4. Generates formatted tickets via TicketFormatter
     * 5. Attempts to print client ticket (max 3 attempts)
     * 6. On print success, persists order with ticket texts in a single transaction
     * 7. On persistence success, resets POS state for next customer
     * 8. On persistence failure, retains state and shows error
     */
    fun confirmPayment() {
        // Guard: do not proceed if cart is empty (Req 8.8)
        if (_cartItems.value.isEmpty()) return

        viewModelScope.launch {
            // Step 1: Set printing state
            _checkoutState.value = _checkoutState.value.copy(isPrinting = true)
            _confirmButtonText.value = "Imprimiendo Ticket"

            // Step 2: Get printer IP
            val ipAddress = printerPreferencesRepository?.getIpAddress() ?: ""

            // Step 3: Validate IP
            if (ipAddress.isEmpty()) {
                _error.value = "No se ha configurado la IP de la impresora"
                _checkoutState.value = _checkoutState.value.copy(isPrinting = false)
                _confirmButtonText.value = "Confirmar Pago"
                return@launch
            }

            // Step 4: Generate ticket data
            val orderId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateTime = dateFormat.format(Date(timestamp))

            // Generate sequential folio for printed tickets
            val orderCount = orderRepository.getOrderCount()
            val folio = (orderCount + 1).toString().padStart(3, '0')

            val checkoutState = _checkoutState.value
            val cartItems = _cartItems.value
            val totalAmount = cartItems.filter { !it.isDivider }.sumOf { it.totalPrice }

            val ticketLineItems = cartItems.map { item ->
                TicketLineItem(
                    quantity = item.quantity,
                    productName = item.productName,
                    lineTotal = item.totalPrice,
                    customizations = item.selectedCustomizations.map { it.optionName },
                    extraNotes = item.extraNotes,
                    isDivider = item.isDivider
                )
            }

            val change = if (checkoutState.paymentStatus == PaymentStatus.PAGADO && checkoutState.cashReceived > 0) {
                BigDecimal(checkoutState.cashReceived)
                    .subtract(BigDecimal(totalAmount))
                    .setScale(2, RoundingMode.HALF_UP)
                    .coerceAtLeast(BigDecimal.ZERO)
                    .toDouble()
            } else 0.0

            val clientTicketText = TicketFormatter.formatClientTicket(
                ticketId = folio,
                dateTime = dateTime,
                customerName = checkoutState.customerName,
                paymentStatus = checkoutState.paymentStatus.displayText,
                items = ticketLineItems,
                totalAmount = totalAmount,
                cashReceived = if (checkoutState.paymentStatus == PaymentStatus.PAGADO) checkoutState.cashReceived else 0.0,
                change = change
            )

            val internalTicketText = TicketFormatter.formatInternalTicket(
                ticketId = folio,
                dateTime = dateTime,
                customerName = checkoutState.customerName,
                paymentStatus = checkoutState.paymentStatus.displayText,
                items = ticketLineItems
            )

            // Generate segmented internal ticket for double-height printing
            val segments = TicketFormatter.formatInternalTicketSegmented(
                ticketId = folio,
                dateTime = dateTime,
                customerName = checkoutState.customerName,
                paymentStatus = checkoutState.paymentStatus.displayText,
                items = ticketLineItems
            )

            // Step 5: Print client ticket (normal text) then internal ticket (double height items)
            try {
                EscPosPrinterLan.printTicket(ipAddress, clientTicketText)
                EscPosPrinterLan.printInternalTicketWithDoubleHeight(
                    ipAddress,
                    segments.header,
                    segments.items,
                    segments.footer
                )
            } catch (e: Exception) {
                // Print failed
                val currentState = _checkoutState.value
                val newAttempts = currentState.printAttempts + 1
                _checkoutState.value = currentState.copy(
                    printAttempts = newAttempts,
                    isPrinting = false
                )

                if (newAttempts >= 3) {
                    _error.value = "No se pudo imprimir después de 3 intentos"
                    _confirmButtonText.value = "Confirmar Pago"
                } else {
                    val errorMsg = e.message ?: "Error desconocido"
                    _error.value = "Error de impresión: $errorMsg"
                    _confirmButtonText.value = "Reintentar"
                }
                return@launch
            }

            // Step 6: Print success — persist order with tickets (Req 12.1-12.4)
            try {
                val orderEntity = OrderEntity(
                    id = orderId,
                    timestamp = timestamp,
                    totalAmount = totalAmount,
                    status = checkoutState.paymentStatus.displayText,
                    customerName = checkoutState.customerName.ifBlank { null },
                    clientTicketText = clientTicketText,
                    internalTicketText = internalTicketText
                )

                val orderItems = cartItems.filter { !it.isDivider }.map { cartItem ->
                    OrderItemEntity(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        productId = cartItem.productId,
                        productName = cartItem.productName,
                        quantity = cartItem.quantity,
                        basePrice = cartItem.basePrice,
                        totalPrice = cartItem.totalPrice,
                        extraNotes = cartItem.extraNotes.ifBlank { null }
                    )
                }

                val customizations = cartItems.filter { !it.isDivider }.mapIndexed { index, cartItem ->
                    cartItem.selectedCustomizations.map { customization ->
                        OrderItemCustomizationEntity(
                            id = UUID.randomUUID().toString(),
                            orderItemId = orderItems[index].id,
                            optionName = customization.optionName,
                            extraPrice = customization.extraPrice
                        )
                    }
                }.flatten()

                orderRepository.persistOrder(
                    order = orderEntity,
                    items = orderItems,
                    customizations = customizations,
                    customerName = checkoutState.customerName.ifBlank { null },
                    clientTicketText = clientTicketText,
                    internalTicketText = internalTicketText
                )

                // Step 7: Persistence success — reset POS state (Req 13.1-13.5)
                resetPosState()
            } catch (e: Exception) {
                // Step 8: Persistence failure — retain state, show error (Req 12.5)
                _checkoutState.value = _checkoutState.value.copy(isPrinting = false)
                _confirmButtonText.value = "Confirmar Pago"
                _error.value = "Error al guardar la orden"
            }
        }
    }

    /**
     * Resets all POS state after a successful order persistence.
     * Clears cart, resets checkout state, hides panels, and restores button text.
     * (Req 13.1-13.5)
     */
    private fun resetPosState() {
        _cartItems.value = emptyList()
        _checkoutState.value = CheckoutState()
        _isCheckoutVisible.value = false
        _isConfirmationModalVisible.value = false
        _confirmButtonText.value = "Confirmar Pago"
    }

    /** Persists the current cart as an order and clears the cart on success. No-op if cart is empty. */
    fun completeOrder() {
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            try {
                val orderId = UUID.randomUUID().toString()
                val nonDividerItems = currentCart.filter { !it.isDivider }
                val orderEntity = OrderEntity(
                    id = orderId,
                    timestamp = System.currentTimeMillis(),
                    totalAmount = nonDividerItems.sumOf { it.totalPrice },
                    status = "PAID"
                )
                val orderItems = nonDividerItems.map { cartItem ->
                    OrderItemEntity(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        productId = cartItem.productId,
                        productName = cartItem.productName,
                        quantity = cartItem.quantity,
                        basePrice = cartItem.basePrice,
                        totalPrice = cartItem.totalPrice,
                        extraNotes = cartItem.extraNotes.ifBlank { null }
                    )
                }
                val customizations = nonDividerItems.mapIndexed { index, cartItem ->
                    cartItem.selectedCustomizations.map { customization ->
                        OrderItemCustomizationEntity(
                            id = UUID.randomUUID().toString(),
                            orderItemId = orderItems[index].id,
                            optionName = customization.optionName,
                            extraPrice = customization.extraPrice
                        )
                    }
                }.flatten()
                orderRepository.persistOrder(orderEntity, orderItems, customizations)
                _cartItems.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al guardar la orden"
            }
        }
    }

    /** Clears the error state after the UI has consumed it. */
    fun clearError() {
        _error.value = null
    }

    // ── Inner Factory ─────────────────────────────────────────────────────────

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val productRepository: ProductRepository,
        private val orderRepository: OrderRepository,
        private val menuId: String,
        private val printerPreferencesRepository: PrinterPreferencesRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PosViewModel(categoryRepository, productRepository, orderRepository, menuId, printerPreferencesRepository) as T
    }
}
