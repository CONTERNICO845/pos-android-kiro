package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Grid of denomination buttons for registering cash received.
 *
 * Bill buttons ($1000, $500, $200, $100, $50, $20) use CardBackground.
 * Coin buttons ($10, $5, $2, $1) use CoinButtonBg (lighter green).
 * Each button shows a Badge with tap count when count > 0.
 *
 * Satisfies Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6
 */
@Composable
fun BillsGrid(
    denominationCounts: Map<Int, Int>,
    onDenominationPressed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bills = listOf(1000, 500, 200, 100, 50, 20)
    val coins = listOf(10, 5, 2, 1)

    val containerShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, containerShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: $1000, $500, $200
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bills.subList(0, 3).forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    BillDenominationButton(
                        denomination = denomination,
                        count = count,
                        isCoin = false,
                        onClick = { onDenominationPressed(denomination) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Row 2: $100, $50, $20
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bills.subList(3, 6).forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    BillDenominationButton(
                        denomination = denomination,
                        count = count,
                        isCoin = false,
                        onClick = { onDenominationPressed(denomination) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Row 3: $10, $5, $2, $1 (coins)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            coins.forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    BillDenominationButton(
                        denomination = denomination,
                        count = count,
                        isCoin = true,
                        onClick = { onDenominationPressed(denomination) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BillDenominationButton(
    denomination: Int,
    count: Int,
    isCoin: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCoin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val textColor = if (isCoin) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge {
                        Text(text = count.toString())
                    }
                }
            }
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = textColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "$$denomination",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
