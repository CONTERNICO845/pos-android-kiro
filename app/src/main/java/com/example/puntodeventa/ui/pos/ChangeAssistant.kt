package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Change assistant panel showing Total, Recibido, and Cambio
 * with an alert box indicating the exact change to give.
 *
 * Uses BigDecimal with HALF_UP rounding for precise currency calculations.
 *
 * Satisfies Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Composable
fun ChangeAssistant(
    cartTotal: Double,
    cashReceived: Double,
    modifier: Modifier = Modifier
) {
    val totalBd = BigDecimal.valueOf(cartTotal).setScale(2, RoundingMode.HALF_UP)
    val receivedBd = BigDecimal.valueOf(cashReceived).setScale(2, RoundingMode.HALF_UP)
    val changeBd = if (receivedBd >= totalBd) {
        receivedBd.subtract(totalBd).setScale(2, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    val alertText = when {
        receivedBd > totalBd -> "Dar \$${changeBd.toPlainString()} de cambio exacto"
        receivedBd.compareTo(totalBd) == 0 -> "Pago exacto"
        else -> {
            val missing = totalBd.subtract(receivedBd).setScale(2, RoundingMode.HALF_UP)
            "Falta \$${missing.toPlainString()}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // 3-column summary: Total | Recibido | Cambio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CurrencyColumn(
                label = "Total",
                value = totalBd.toPlainString(),
                modifier = Modifier.weight(1f)
            )
            CurrencyColumn(
                label = "Recibido",
                value = receivedBd.toPlainString(),
                modifier = Modifier.weight(1f)
            )
            CurrencyColumn(
                label = "Cambio",
                value = changeBd.toPlainString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Alert box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = alertText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun CurrencyColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$$value",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}
