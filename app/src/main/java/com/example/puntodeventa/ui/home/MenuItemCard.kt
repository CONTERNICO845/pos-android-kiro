package com.example.puntodeventa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.ui.theme.CardBackground
import com.example.puntodeventa.ui.theme.CardIconTint
import com.example.puntodeventa.ui.theme.CardText

@Composable
fun MenuItemCard(
    item: MenuItem,
    onEditClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
    ) {
        // Edit icon — top-right corner
        IconButton(
            onClick  = { onEditClick(item) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Edit,
                contentDescription = "Editar ${item.name}",
                tint               = CardIconTint,
                modifier           = Modifier.size(20.dp)
            )
        }

        // Card content — centered
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text     = item.emoji,
                fontSize = 64.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = item.name.uppercase(),
                color      = CardText,
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
