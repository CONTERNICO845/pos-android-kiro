package com.example.puntodeventa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onNavigateToPOS: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopStart
    ) {
        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp),
            modifier              = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Existing menu items
            items(uiState.menuItems, key = { it.id }) { item ->
                MenuItemCard(
                    item        = item,
                    onClick     = { onNavigateToPOS(item.id) },
                    onEditClick = { viewModel.openEditDialog(it) }
                )
            }

            // "+" card — always last
            item {
                AddMenuCard(onClick = { viewModel.openDialog() })

            }
        }
    }

    // Dialog — shown on top when isDialogOpen is true
    if (uiState.isDialogOpen) {
        // Capture in a local val so Kotlin can smart-cast inside the lambda.
        // uiState.editingItem is a delegated property and cannot be smart-cast directly.
        val editingItem = uiState.editingItem

        AddMenuDialog(
            editingItem = editingItem,
            onSave      = { emoji, name -> viewModel.saveMenu(emoji, name) },
            // onDelete is only provided in edit mode (editingItem != null).
            // In create mode it is null, so the delete button is entirely absent.
            onDelete    = if (editingItem != null) {
                {
                    viewModel.deleteMenu(editingItem.id)
                    viewModel.dismissDialog()
                }
            } else null,
            onDismiss   = { viewModel.dismissDialog() }
        )
    }
}
