package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.puntodeventa.data.model.MenuItem
import androidx.compose.material3.MaterialTheme

@Composable
fun MenuFilterBar(
    menuItems: List<MenuItem>,
    selectedMenuId: String?,
    onMenuSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (menuItems.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(menuItems, key = { it.id }) { menuItem ->
            val isSelected = menuItem.id == selectedMenuId
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .clickable {
                        if (isSelected) {
                            onMenuSelected(null)
                        } else {
                            onMenuSelected(menuItem.id)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${menuItem.emoji} ${menuItem.name}",
                    color = textColor
                )
            }
        }
    }
}
