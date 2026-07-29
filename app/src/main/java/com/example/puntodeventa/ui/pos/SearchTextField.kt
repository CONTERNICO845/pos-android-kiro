package com.example.puntodeventa.ui.pos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme

private const val MAX_QUERY_LENGTH = 100

/**
 * Search text field for filtering products by name in the POS screen.
 *
 * Displays a magnifying glass leading icon and a clear (X) trailing icon
 * when the query is non-empty. Enforces a maximum length of 100 characters.
 *
 * Satisfies Requirements: 2.1, 2.2, 2.5
 */
@Composable
fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = { newValue ->
            if (newValue.length <= MAX_QUERY_LENGTH) {
                onQueryChange(newValue)
            }
        },
        modifier = modifier,
        placeholder = {
            Text(
                text = "Buscar producto...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda"
                    )
                }
            }
        },
        singleLine = true
    )
}
