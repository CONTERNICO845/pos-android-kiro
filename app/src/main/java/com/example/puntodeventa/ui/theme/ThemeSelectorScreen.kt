package com.example.puntodeventa.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Screen that displays available themes in a 2-column grid.
 * Accessible from the Settings area via "Apariencia" navigation.
 *
 * @param currentTheme The currently active [AppTheme].
 * @param onThemeSelected Callback invoked when the user taps a theme card.
 */
@Composable
fun ThemeSelectorScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title "Apariencia" spanning full width above the grid items
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // One ThemeCard per AppTheme entry
        items(AppTheme.entries.toList()) { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme == currentTheme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}
