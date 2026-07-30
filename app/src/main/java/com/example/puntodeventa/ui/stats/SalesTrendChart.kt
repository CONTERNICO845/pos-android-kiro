package com.example.puntodeventa.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/** Rendering style of the sales trend chart. (Req 8.8) */
enum class ChartMode(val label: String) {
    BAR("Barras"),
    LINE("Línea")
}

private val PLOT_HEIGHT = 180.dp
private val LEFT_GUTTER = 56.dp
private val BOTTOM_GUTTER = 22.dp
private val MIN_LABEL_SLOT = 22.dp

/**
 * Interactive revenue-per-bucket chart drawn on a [Canvas]. (Req 8.1, 8.3, 8.6, 8.7, 8.9)
 *
 * Implemented by hand instead of with a charting library so every color resolves from
 * [MaterialTheme.colorScheme] and the app's 9 themes propagate without adapter code.
 *
 * Tapping a bucket selects it and shows its full label and revenue above the plot; tapping the
 * selected bucket again — or anywhere outside the plot — clears the selection. The selection resets
 * whenever [series] changes, which is what happens on a time filter change. (Req 8.10)
 */
@Composable
fun SalesTrendChart(
    series: List<SalesTrendPoint>,
    mode: ChartMode,
    modifier: Modifier = Modifier
) {
    val maxRevenue = series.maxOfOrNull { it.revenue } ?: 0.0

    if (series.isEmpty() || maxRevenue <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = PLOT_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin ventas en este periodo",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        return
    }

    var selectedIndex by remember(series) { mutableStateOf<Int?>(null) }

    val axisColor = MaterialTheme.colorScheme.outline
    val barColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onBackground
    val textMeasurer = rememberTextMeasurer()

    val axisTextStyle = TextStyle(
        color = labelColor.copy(alpha = 0.7f),
        fontSize = 10.sp
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip lives above the plot so it can never overlap the bars. (Req 8.7)
        val selected = selectedIndex?.let(series::getOrNull)
        Text(
            text = if (selected != null) {
                "${selected.fullLabel} · ${StatsFormatters.formatCurrency(selected.revenue)}"
            } else {
                "Toca una barra para ver el detalle"
            },
            color = if (selected != null) labelColor else labelColor.copy(alpha = 0.6f),
            fontWeight = if (selected != null) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLOT_HEIGHT + BOTTOM_GUTTER)
                .pointerInput(series, mode) {
                    detectTapGestures { offset ->
                        val leftGutterPx = LEFT_GUTTER.toPx()
                        val plotWidth = size.width - leftGutterPx
                        val plotHeight = size.height - BOTTOM_GUTTER.toPx()
                        val tapped = bucketIndexAt(
                            x = offset.x,
                            y = offset.y,
                            plotLeft = leftGutterPx,
                            plotWidth = plotWidth,
                            plotHeight = plotHeight,
                            bucketCount = series.size
                        )
                        // Re-tapping the current selection clears it, same as tapping outside.
                        selectedIndex = if (tapped == null || tapped == selectedIndex) null else tapped
                    }
                }
        ) {
            val leftGutterPx = LEFT_GUTTER.toPx()
            val plotWidth = size.width - leftGutterPx
            val plotHeight = size.height - BOTTOM_GUTTER.toPx()
            if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

            val slotWidth = plotWidth / series.size

            // ── Grid lines + Y axis labels (0, half, max) ─────────────────────
            val gridLines = 4
            repeat(gridLines + 1) { index ->
                val y = plotHeight - plotHeight * index / gridLines
                drawLine(
                    color = axisColor.copy(alpha = 0.25f),
                    start = Offset(leftGutterPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            listOf(0, gridLines / 2, gridLines).forEach { index ->
                val value = maxRevenue * index / gridLines
                val layout = textMeasurer.measure(
                    text = StatsFormatters.formatCompactCurrency(value),
                    style = axisTextStyle
                )
                val y = plotHeight - plotHeight * index / gridLines
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = (leftGutterPx - 6.dp.toPx() - layout.size.width).coerceAtLeast(0f),
                        y = y - layout.size.height / 2f
                    )
                )
            }

            // ── Series ────────────────────────────────────────────────────────
            fun barHeight(revenue: Double): Float =
                (revenue / maxRevenue).toFloat().coerceIn(0f, 1f) * plotHeight

            when (mode) {
                ChartMode.BAR -> {
                    val barWidth = slotWidth * 0.62f
                    series.forEachIndexed { index, point ->
                        val height = barHeight(point.revenue)
                        if (height <= 0f) return@forEachIndexed
                        val left = leftGutterPx + slotWidth * index + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = if (index == selectedIndex) highlightColor else barColor,
                            topLeft = Offset(left, plotHeight - height),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }

                ChartMode.LINE -> {
                    val centers = series.mapIndexed { index, point ->
                        Offset(
                            x = leftGutterPx + slotWidth * index + slotWidth / 2f,
                            y = plotHeight - barHeight(point.revenue)
                        )
                    }
                    // Area under the line, faded to transparent at the baseline.
                    val area = Path().apply {
                        moveTo(centers.first().x, plotHeight)
                        centers.forEach { lineTo(it.x, it.y) }
                        lineTo(centers.last().x, plotHeight)
                        close()
                    }
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = 0.28f), Color.Transparent),
                            startY = 0f,
                            endY = plotHeight
                        )
                    )
                    val line = Path().apply {
                        moveTo(centers.first().x, centers.first().y)
                        centers.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = line,
                        color = barColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    centers.forEachIndexed { index, center ->
                        val isSelected = index == selectedIndex
                        drawCircle(
                            color = if (isSelected) highlightColor else barColor,
                            radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                            center = center
                        )
                    }
                }
            }

            // ── X axis labels, thinned to whatever fits ────────────────────────
            val step = ceil(MIN_LABEL_SLOT.toPx() / slotWidth).toInt().coerceAtLeast(1)
            series.forEachIndexed { index, point ->
                val isEdge = index == 0 || index == series.lastIndex
                if (!isEdge && index % step != 0) return@forEachIndexed
                val layout = textMeasurer.measure(text = point.label, style = axisTextStyle)
                val center = leftGutterPx + slotWidth * index + slotWidth / 2f
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = (center - layout.size.width / 2f)
                            .coerceIn(leftGutterPx, size.width - layout.size.width),
                        y = plotHeight + 4.dp.toPx()
                    )
                )
            }
        }
    }
}

/**
 * Maps a tap to a bucket index, or `null` when the tap lands outside the plot area.
 * Kept separate so the mapping is trivially reviewable and reusable by both chart modes.
 */
private fun bucketIndexAt(
    x: Float,
    y: Float,
    plotLeft: Float,
    plotWidth: Float,
    plotHeight: Float,
    bucketCount: Int
): Int? {
    if (bucketCount <= 0 || plotWidth <= 0f) return null
    if (x < plotLeft || x > plotLeft + plotWidth) return null
    if (y < 0f || y > plotHeight) return null
    val index = ((x - plotLeft) / (plotWidth / bucketCount)).toInt()
    return index.coerceIn(0, bucketCount - 1)
}
