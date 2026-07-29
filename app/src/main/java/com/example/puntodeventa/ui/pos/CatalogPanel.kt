package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.puntodeventa.data.model.Category
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.data.model.Product
import androidx.compose.material3.MaterialTheme

/**
 * The catalog panel composing the MenuFilterBar, CategoryTabBar, optional SearchTextField,
 * and the ProductGrid.
 *
 * This panel occupies the left portion of the POS screen (width allocation handled by parent).
 * It fills its given space vertically, with the filter bars taking their intrinsic height
 * and the product grid filling the remaining vertical space.
 *
 * Satisfies Requirements: 1.1, 2.1, 2.2, 2.6, 3.1
 */
@Composable
fun CatalogPanel(
    categories: List<Category>,
    selectedCategory: Category?,
    products: List<Product>,
    menuItems: List<MenuItem>,
    selectedMenuId: String?,
    searchQuery: String,
    isSearchVisible: Boolean,
    onCategorySelected: (Category?) -> Unit,
    onProductTapped: (Product) -> Unit,
    onMenuSelected: (String?) -> Unit,
    onSearchClick: () -> Unit,
    onDividerClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Menu filter bar above the category tabs (Req 1.1)
        MenuFilterBar(
            menuItems = menuItems,
            selectedMenuId = selectedMenuId,
            onMenuSelected = onMenuSelected,
            modifier = Modifier.fillMaxWidth()
        )

        // Category tab bar with search and scissors icons (Req 2.1, 2.6, 3.1)
        CategoryTabBar(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            onSearchClick = onSearchClick,
            onDividerClick = onDividerClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Search text field below CategoryTabBar, visible when toggled (Req 2.1)
        if (isSearchVisible) {
            SearchTextField(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onClear = onClearSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        ProductGrid(
            products = products,
            onProductTapped = onProductTapped,
            modifier = Modifier.weight(1f)
        )
    }
}
