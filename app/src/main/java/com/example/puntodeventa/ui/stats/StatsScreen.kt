package com.example.puntodeventa.ui.stats

import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.data.local.OrderEntity
import com.example.puntodeventa.data.model.ProductSaleSummary

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onFilterChange: (TimeFilter) -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit,
    onDateRangePickerDismissed: () -> Unit,
    onChartModeChange: (ChartMode) -> Unit,
    onExportUriReceived: (Uri, ContentResolver) -> Unit,
    onUserMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // SAF destination picker for the CSV report — same contract used by the JSON catalog export.
    // (Req 15.2, 15.10: a cancelled picker returns a null Uri and nothing happens.)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportUriReceived(uri, context.contentResolver)
        }
    }

    // Transient messages are shown once and then cleared. (Req 16.3)
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onUserMessageShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        StatsTopBar(
            selectedFilter = uiState.selectedFilter,
            isExporting = uiState.isExporting,
            onFilterChange = onFilterChange,
            onExportClick = { exportLauncher.launch(StatsViewModel.exportFileName()) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            MetricCardsRow(uiState = uiState)
            Spacer(modifier = Modifier.height(16.dp))

            // Trend chart gets the larger share of the width. (Req 11.8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SalesTrendSection(
                    uiState = uiState,
                    onChartModeChange = onChartModeChange,
                    modifier = Modifier.weight(1.6f)
                )
                PaymentBreakdownSection(
                    slices = uiState.paymentBreakdown,
                    totalRevenue = uiState.totalRevenue,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom two-column section: Top Products (left) and Recent Orders (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TopProductsSection(
                    topProducts = uiState.topProducts,
                    modifier = Modifier.weight(1f)
                )
                RecentOrdersSection(
                    recentOrders = uiState.recentOrders,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // DateRangePicker dialog
    if (uiState.showDateRangePicker) {
        DateRangePickerDialog(
            onConfirm = onDateRangeSelected,
            onDismiss = onDateRangePickerDismissed,
            initialStartMillis = uiState.customStartMillis,
            initialEndMillis = uiState.customEndMillis
        )
    }
}

@Composable
private fun StatsTopBar(
    selectedFilter: TimeFilter,
    isExporting: Boolean,
    onFilterChange: (TimeFilter) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: title and subtitle
        Column {
            Text(
                text = "Estadísticas",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = "Resumen de ventas y métricas",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
        }

        // Right side: segmented filter selector + CSV export action
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeFilterSelector(
                selectedFilter = selectedFilter,
                onFilterChange = onFilterChange
            )
            Spacer(modifier = Modifier.size(8.dp))
            IconButton(
                onClick = onExportClick,
                enabled = !isExporting   // no concurrent writes of the same report (Req 16.2)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Exportar Reporte",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = if (isExporting) 0.4f else 1f
                    )
                )
            }
        }
    }
}

@Composable
private fun TimeFilterSelector(
    selectedFilter: TimeFilter,
    onFilterChange: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val displayLabel = if (filter == TimeFilter.CUSTOM) "📅 Rango" else filter.label
            Text(
                text = displayLabel,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onFilterChange(filter) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}


@Composable
private fun MetricCardsRow(
    uiState: StatsUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            label = "INGRESOS",
            value = StatsFormatters.formatCurrency(uiState.totalRevenue),
            delta = uiState.revenueDelta,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "ÓRDENES",
            value = StatsFormatters.formatCount(uiState.orderCount),
            delta = uiState.orderCountDelta,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "TICKET PROMEDIO",
            value = StatsFormatters.formatCurrency(uiState.averageTicket),
            delta = uiState.averageTicketDelta,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "CLIENTES",
            value = uiState.customerCount.toString(),
            delta = uiState.customerCountDelta,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    delta: MetricDelta,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        // The indicator is hidden entirely when the filter has no baseline. (Req 13.2)
        if (delta.available) {
            Spacer(modifier = Modifier.height(6.dp))
            MetricDeltaChip(delta = delta)
        }
    }
}

/**
 * Direction arrow plus percentage change against the previous period.
 * (Req 13.4, 13.5, 13.6, 13.7, 13.8, 13.10)
 *
 * Colors come from the theme: `tertiary` is the positive accent and `error` the negative one, so the
 * chip stays legible across all 9 themes without hard-coded green/red literals.
 */
@Composable
private fun MetricDeltaChip(
    delta: MetricDelta,
    modifier: Modifier = Modifier
) {
    val neutralColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val tint = when (delta.direction) {
        TrendDirection.UP, TrendDirection.NO_BASELINE -> MaterialTheme.colorScheme.tertiary
        TrendDirection.DOWN -> MaterialTheme.colorScheme.error
        TrendDirection.FLAT -> neutralColor
    }
    val icon = when (delta.direction) {
        TrendDirection.UP, TrendDirection.NO_BASELINE -> Icons.Default.ArrowUpward
        TrendDirection.DOWN -> Icons.Default.ArrowDownward
        TrendDirection.FLAT -> Icons.Default.Remove
    }
    val text = delta.percent
        ?.let { StatsFormatters.formatPercent(it) }
        ?: "Nuevo"

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = text,
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "vs periodo anterior",
            color = neutralColor,
            fontSize = 10.sp
        )
    }
}


@Composable
private fun SalesTrendSection(
    uiState: StatsUiState,
    onChartModeChange: (ChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tendencia de ventas",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Por ${uiState.trendGranularity.csvLabel.lowercase()}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            ChartModeToggle(
                selectedMode = uiState.chartMode,
                onChartModeChange = onChartModeChange
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SalesTrendChart(
            series = uiState.trendSeries,
            mode = uiState.chartMode
        )
    }
}

/** Bar/line switch for the trend chart. (Req 8.8) */
@Composable
private fun ChartModeToggle(
    selectedMode: ChartMode,
    onChartModeChange: (ChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            Text(
                text = mode.label,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onChartModeChange(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}


@Composable
private fun PaymentBreakdownSection(
    slices: List<PaymentSlice>,
    totalRevenue: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Ventas por método de pago",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        PaymentMethodDonut(
            slices = slices,
            totalRevenue = totalRevenue
        )
    }
}


@Composable
private fun TopProductsSection(
    topProducts: List<ProductSaleSummary>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Productos más vendidos",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (topProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin datos para este periodo",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else {
            topProducts.forEach { product ->
                TopProductRow(product = product)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopProductRow(
    product: ProductSaleSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = product.productName,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = StatsFormatters.formatQuantitySold(product.totalQuantity),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Text(
                text = StatsFormatters.formatCurrency(product.totalRevenue),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
private fun RecentOrdersSection(
    recentOrders: List<OrderEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Órdenes recientes",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (recentOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin órdenes para este periodo",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else {
            recentOrders.forEach { order ->
                RecentOrderRow(order = order)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecentOrderRow(
    order: OrderEntity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = StatsFormatters.formatOrderTime(order.timestamp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 13.sp
        )
        Text(
            text = StatsFormatters.displayCustomerName(order.customerName),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = StatsFormatters.formatCurrency(order.totalAmount),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
