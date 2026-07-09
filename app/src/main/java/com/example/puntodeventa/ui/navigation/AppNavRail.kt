package com.example.puntodeventa.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.ui.theme.NavRailBackground
import com.example.puntodeventa.ui.theme.NavRailIconDefault
import com.example.puntodeventa.ui.theme.NavRailIconSelected

private data class RailItem(
    val destination: NavDestination,
    val icon: ImageVector,
)

private val railItems = listOf(
    RailItem(NavDestination.Home,     Icons.Default.Home),
    RailItem(NavDestination.Stats,    Icons.Default.ShowChart),
    RailItem(NavDestination.Settings, Icons.Default.Settings),
    RailItem(NavDestination.Tickets,  Icons.Default.ConfirmationNumber),
    RailItem(NavDestination.Printer,  Icons.Default.Print),
)

@Composable
fun AppNavRail(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .background(NavRailBackground),
        containerColor = NavRailBackground,
    ) {
        railItems.forEach { item ->
            val selected = currentDestination == item.destination
            NavigationRailItem(
                selected  = selected,
                onClick   = { onDestinationSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.destination.label,
                        tint               = if (selected) NavRailIconSelected else NavRailIconDefault,
                        modifier           = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        text       = item.destination.label,
                        fontSize   = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                        color      = if (selected) NavRailIconSelected else NavRailIconDefault,
                        maxLines   = 2,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = NavRailBackground,
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
