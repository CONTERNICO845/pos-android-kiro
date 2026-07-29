package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.puntodeventa.data.model.Category
import androidx.compose.material3.MaterialTheme

/**
 * A horizontally scrollable category tab bar for the POS screen.
 *
 * Displays a "TODO" tab first (representing all products), followed by categories
 * sorted alphabetically. Includes fixed trailing search and split-bill icons.
 *
 * Satisfies Requirements: 2.1, 2.6, 3.1, 3.2, 3.3, 3.6, 3.7, 3.8, 3.9
 */
@Composable
fun CategoryTabBar(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    onSearchClick: () -> Unit = {},
    onDividerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabOrder = buildTabOrder(categories)
    val selectedIndex = tabOrder.indexOfFirst { it?.id == selectedCategory?.id }
        .coerceAtLeast(0)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scrollable tabs taking available horizontal space
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primaryContainer // suppress indicator by matching container
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            tabOrder.forEachIndexed { index, category ->
                val isSelected = index == selectedIndex
                Tab(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    text = {
                        Text(
                            text = category?.name ?: "TODO",
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Fixed trailing icons: search and split-bill
        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        IconButton(onClick = onDividerClick) {
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = "Dividir cuenta",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
