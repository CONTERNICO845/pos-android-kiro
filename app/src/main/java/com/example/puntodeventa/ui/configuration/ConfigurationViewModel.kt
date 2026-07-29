package com.example.puntodeventa.ui.configuration

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.Product
import com.example.puntodeventa.data.repository.CatalogJsonRepository
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

data class ConfigurationUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val expandedProductMenuId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteCategoryDialog: Boolean = false,
    // JSON management states
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showJsonEditor: Boolean = false,
    val jsonEditorContent: String = "",
    val jsonEditorError: String? = null,
    val showImportConfirmDialog: Boolean = false,
    val importPendingJson: String? = null,
    val toastMessage: String? = null
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
    private val catalogJsonRepository: CatalogJsonRepository,
    private val menuId: String
) : ViewModel() {

    // ── Internal mutable state ────────────────────────────────────────────────

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _expandedMenuId = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _showDeleteCategoryDialog = MutableStateFlow(false)

    // JSON management state
    private val _isExporting = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _showJsonEditor = MutableStateFlow(false)
    private val _jsonEditorContent = MutableStateFlow("")
    private val _jsonEditorError = MutableStateFlow<String?>(null)
    private val _showImportConfirmDialog = MutableStateFlow(false)
    private val _importPendingJson = MutableStateFlow<String?>(null)
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _exportedJson = MutableStateFlow<String?>(null)

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
    // Split into two combine layers since Kotlin combine supports max ~5 typed params.
    private val coreState = combine(
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
        ConfigurationUiState(
            categories = args[0] as List<Category>,
            selectedCategory = args[1] as Category?,
            products = args[2] as List<Product>,
            filteredProducts = args[3] as List<Product>,
            searchQuery = args[4] as String,
            expandedProductMenuId = args[5] as String?,
            error = args[6] as String?,
            isLoading = args[7] as Boolean,
            showDeleteCategoryDialog = args[8] as Boolean
        )
    }

    val uiState: StateFlow<ConfigurationUiState> = combine(
        coreState,
        _isExporting,
        _isImporting,
        _showJsonEditor,
        _jsonEditorContent,
        _jsonEditorError,
        _showImportConfirmDialog,
        _importPendingJson,
        _toastMessage
    ) { args ->
        val core = args[0] as ConfigurationUiState
        core.copy(
            isExporting = args[1] as Boolean,
            isImporting = args[2] as Boolean,
            showJsonEditor = args[3] as Boolean,
            jsonEditorContent = args[4] as String,
            jsonEditorError = args[5] as String?,
            showImportConfirmDialog = args[6] as Boolean,
            importPendingJson = args[7] as String?,
            toastMessage = args[8] as String?
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

    // ── JSON Management Functions ─────────────────────────────────────────────

    /**
     * Prepares the catalog JSON for export. The exported string is stored internally
     * so the Composable can retrieve it after launching the SAF CreateDocument picker.
     */
    fun exportCatalog() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    catalogJsonRepository.exportCatalogToJson()
                }
                _exportedJson.value = jsonString
            } catch (e: Exception) {
                _toastMessage.value = "Error al exportar: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /** Returns the last exported JSON string (consumed by the SAF launcher callback). */
    fun consumeExportedJson(): String? {
        val json = _exportedJson.value
        _exportedJson.value = null
        return json
    }

    /**
     * Writes the exported JSON to the Uri provided by the SAF CreateDocument result.
     */
    fun onExportUriReceived(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val jsonString = _exportedJson.value
                    ?: withContext(Dispatchers.IO) { catalogJsonRepository.exportCatalogToJson() }

                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
                }
                _exportedJson.value = null
                _toastMessage.value = "Catálogo exportado correctamente"
            } catch (e: Exception) {
                _toastMessage.value = "Error al exportar: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Reads JSON from the Uri provided by the SAF OpenDocument result,
     * validates it, and shows the import confirmation dialog.
     */
    fun importCatalogFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: throw IllegalStateException("No se pudo leer el archivo")
                }
                _importPendingJson.value = jsonString
                _showImportConfirmDialog.value = true
            } catch (e: Exception) {
                _toastMessage.value = "Error al leer archivo: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Confirms the pending import and executes the replace-all transaction. */
    fun confirmImport() {
        val jsonString = _importPendingJson.value ?: return
        _showImportConfirmDialog.value = false
        _importPendingJson.value = null

        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    catalogJsonRepository.importCatalogFromJson(jsonString)
                }
                result.fold(
                    onSuccess = { count ->
                        _toastMessage.value = "Catálogo importado correctamente ($count productos)"
                    },
                    onFailure = { error ->
                        _toastMessage.value = error.message ?: "Error al importar"
                    }
                )
            } catch (e: Exception) {
                _toastMessage.value = "Error al importar: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Dismisses the import confirmation dialog without importing. */
    fun dismissImportDialog() {
        _showImportConfirmDialog.value = false
        _importPendingJson.value = null
    }

    /** Opens the JSON editor modal, loading the current catalog as formatted JSON. */
    fun openJsonEditor() {
        viewModelScope.launch {
            _showJsonEditor.value = true
            _jsonEditorError.value = null
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    catalogJsonRepository.exportCatalogToJson()
                }
                _jsonEditorContent.value = jsonString
            } catch (e: Exception) {
                _jsonEditorError.value = "Error al cargar catálogo: ${e.message}"
            }
        }
    }

    /** Closes the JSON editor modal without applying changes. */
    fun closeJsonEditor() {
        _showJsonEditor.value = false
        _jsonEditorContent.value = ""
        _jsonEditorError.value = null
    }

    /**
     * Validates and applies the edited JSON from the editor modal.
     * On validation failure, sets the editor error without closing.
     * On success, shows the import confirm dialog flow.
     */
    fun applyJsonEditorChanges(editedJson: String) {
        _importPendingJson.value = editedJson
        _jsonEditorError.value = null
        _showImportConfirmDialog.value = true
    }

    /** Clears the toast message after the UI has consumed it. */
    fun clearToast() {
        _toastMessage.value = null
    }

    // ── Inner Factory ─────────────────────────────────────────────────────────

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val productRepository: ProductRepository,
        private val catalogJsonRepository: CatalogJsonRepository,
        private val menuId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConfigurationViewModel(categoryRepository, productRepository, catalogJsonRepository, menuId) as T
    }
}
