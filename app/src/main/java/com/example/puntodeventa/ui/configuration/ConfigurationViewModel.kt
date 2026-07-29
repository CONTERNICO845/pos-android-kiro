package com.example.puntodeventa.ui.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfigurationUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val expandedProductMenuId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteCategoryDialog: Boolean = false
)

internal fun applyFilter(products: List<Product>, query: String): List<Product> {
    val trimmed = query.trim().take(100)
    return if (trimmed.isBlank()) products
    else products.filter { it.name.contains(trimmed, ignoreCase = true) }
}

internal fun clampQuery(query: String): String = query.take(100)

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationViewModel(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val menuId: String
) : ViewModel() {

    // ── Internal mutable state ────────────────────────────────────────────────

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _expandedMenuId = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _showDeleteCategoryDialog = MutableStateFlow(false)

    // ── Reactive pipeline ─────────────────────────────────────────────────────

    // Step 1 — Share the categories Flow so we can both run the side-effect and
    //           feed it into the combine without opening two DB subscriptions.
    private val categoriesFlow = categoryRepository.getCategoriesByMenu(menuId)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1
        )

    init {
        // Auto-select first category on first emission and whenever the current
        // selection disappears from the list (AC-02.5, AC-02.8).
        categoriesFlow
            .onEach { cats ->
                val current = _selectedCategory.value
                if (current == null || cats.none { it.id == current.id }) {
                    _selectedCategory.value = cats.firstOrNull()
                }
                // isLoading stays true until the first category emission arrives.
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    // Step 2 — flatMapLatest: cancel previous product Flow when category changes (AC-09.2).
    private val rawProducts = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) flowOf(emptyList())
            else productRepository.getProductsByCategory(category.id)
        }

    // Step 3 — combine(rawProducts, searchQuery) → filteredProducts (AC-04.2, AC-04.3).
    private val filteredProducts = combine(rawProducts, _searchQuery) { products, query ->
        applyFilter(products, query)
    }

    // Step 4 — combine all streams into a single ConfigurationUiState (AC-09.1, AC-09.6).
    val uiState: StateFlow<ConfigurationUiState> = combine(
        categoriesFlow,
        _selectedCategory,
        rawProducts,
        filteredProducts,
        _searchQuery,
        _expandedMenuId,
        _error,
        _isLoading,
        _showDeleteCategoryDialog
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val categories = args[0] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val selectedCategory = args[1] as Category?
        @Suppress("UNCHECKED_CAST")
        val products = args[2] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val filtered = args[3] as List<Product>
        val searchQuery = args[4] as String
        val expandedMenuId = args[5] as String?
        val error = args[6] as String?
        val isLoading = args[7] as Boolean
        val showDeleteCategoryDialog = args[8] as Boolean

        ConfigurationUiState(
            categories = categories,
            selectedCategory = selectedCategory,
            products = products,
            filteredProducts = filtered,
            searchQuery = searchQuery,
            expandedProductMenuId = expandedMenuId,
            isLoading = isLoading,
            error = error,
            showDeleteCategoryDialog = showDeleteCategoryDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ConfigurationUiState(isLoading = true)
    )

    // ── Public write functions ────────────────────────────────────────────────

    /** Selects a category and clears the search query (AC-04.4). */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    /**
     * Updates the search query, clamping to 100 characters (AC-04.2).
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = clampQuery(query)
    }

    /**
     * Toggles the isActive flag of the given product and persists it (AC-07.4).
     */
    fun toggleProductActive(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.insert(product.copy(isActive = !product.isActive))
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            }
        }
    }

    /**
     * Duplicates the product with a deep copy of all its customization data (AC-08.5).
     * All three entity levels (product, groups, options) are copied inside a single Room
     * transaction with fresh UUIDs at every level.
     * Dismisses the menu on success; on exception sets error and also dismisses (AC-08.6).
     */
    fun duplicateProduct(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.deepCopyProduct(product)
                _expandedMenuId.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
                _expandedMenuId.value = null
            }
        }
    }

    /**
     * Deletes the product by id (AC-08.7).
     * Dismisses the menu immediately before the coroutine runs.
     * On exception sets error; product remains visible (AC-08.8).
     */
    fun deleteProduct(productId: String) {
        _expandedMenuId.value = null
        viewModelScope.launch {
            try {
                productRepository.deleteById(productId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            }
        }
    }

    /**
     * Opens or closes the DropdownMenu for a given product (AC-08.9).
     * Passing null closes any open menu.
     */
    fun setExpandedProductMenu(productId: String?) {
        _expandedMenuId.value = productId
    }

    /** Clears the error state after the UI has consumed it. */
    fun clearError() {
        _error.value = null
    }

    /**
     * Opens the delete category confirmation dialog (AC-02.3).
     */
    fun requestDeleteCategory() {
        _showDeleteCategoryDialog.value = true
    }

    /**
     * Cancels the delete category operation and closes the dialog (AC-02.6).
     */
    fun dismissDeleteCategoryDialog() {
        _showDeleteCategoryDialog.value = false
    }

    /**
     * Confirms and executes the category deletion (AC-02.7, AC-02.8, AC-02.10, AC-02.11).
     * Closes the dialog immediately, then asynchronously deletes the category.
     * On success, the categoriesFlow.onEach block auto-selects the next category (AC-02.9).
     * On error, preserves selectedCategory and sets error message.
     */
    fun confirmDeleteCategory() {
        val categoryToDelete = _selectedCategory.value ?: return
        _showDeleteCategoryDialog.value = false
        viewModelScope.launch {
            try {
                categoryRepository.deleteById(categoryToDelete.id)
                // selectedCategory is auto-updated via categoriesFlow.onEach
                // which calls _selectedCategory.value = cats.firstOrNull()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            }
        }
    }

    // ── Inner Factory ─────────────────────────────────────────────────────────

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val productRepository: ProductRepository,
        private val menuId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConfigurationViewModel(categoryRepository, productRepository, menuId) as T
    }
}
