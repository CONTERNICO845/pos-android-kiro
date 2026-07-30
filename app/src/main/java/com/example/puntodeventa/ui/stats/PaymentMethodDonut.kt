package com.example.puntodeventa.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.data.model.PaymentMethod

/** One donut slice: a resolved tender type with its revenue, order count and share of the period. */
data class PaymentSlice(
    val method: PaymentMethod,
    val revenue: Double,
    val orderCount: Int,
    val share: Double
)

private val DONUT_SIZE = 148.dp
private val DONUT_STROKE = 24.dp
private const val SLICE_GAP_DEGREES = 2f

/**
 * Revenue distribution by tender type: donut plus legend. (Req 14.5, 14.6, 14.7, 14.8)
 *
 * Slice colors are taken by position from a themed palette so a theme swap recolors both the arcs
 * and the legend consistently, with no hard-coded color literals. (Req 11.7)
 */
@Composable
fun PaymentMethodDonut(
    slices: List<PaymentSlice>,
    totalRevenue: Double,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty() || totalRevenue <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
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

    val palette = slicePalette()
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(DONUT_SIZE),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(DONUT_SIZE)) {
                val strokePx = DONUT_STROKE.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                // A single slice draws a full ring; multiple slices leave a small visual gap.
                val gap = if (slices.size > 1) SLICE_GAP_DEGREES else 0f
                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = (slice.share * 360.0).toFloat()
                    if (sweep <= 0f) return@forEachIndexed
                    drawArc(
                        color = palette[index % palette.size],
                        startAngle = startAngle + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = StatsFormatters.formatCurrency(totalRevenue),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Total",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        slices.forEachIndexed { index, slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette[index % palette.size])
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = slice.method.displayName,
                    color = textColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = StatsFormatters.formatCurrency(slice.revenue),
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${StatsFormatters.formatShare(slice.share * 100.0)} · " +
                            StatsFormatters.formatCount(slice.orderCount),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Themed slice colors, ordered by slice position. */
@Composable
private fun slicePalette(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.outline
)
