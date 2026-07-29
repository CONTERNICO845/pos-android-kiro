package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Cash denomination keypad for registering cash received from customers.
 *
 * Displays a grid of denomination buttons ($1000 down to $1) with badge indicators
 * showing press counts, a formatted cumulative cash received display, and a clear button.
 *
 * Uses a static Column+Row layout instead of LazyVerticalGrid to avoid nested scrolling
 * violations when placed inside a vertically scrollable parent (CheckoutPanel).
 *
 * Satisfies Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Composable
fun CashKeypad(
    denominationCounts: Map<Int, Int>,
    cashReceived: Double,
    onDenominationPressed: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val denominations = listOf(1000, 500, 200, 100, 50, 20, 10, 5, 2, 1)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cash received display
        Text(
            text = "$${String.format("%.2f", cashReceived)}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Row 1: $1000, $500, $200, $100
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            denominations.subList(0, 4).forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    DenominationButton(
                        denomination = denomination,
                        count = count,
                        onClick = { onDenominationPressed(denomination) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 2.dp))

        // Row 2: $50, $20, $10, $5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            denominations.subList(4, 8).forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    DenominationButton(
                        denomination = denomination,
                        count = count,
                        onClick = { onDenominationPressed(denomination) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 2.dp))

        // Row 3: $2, $1 + 2 empty spacers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            denominations.subList(8, 10).forEach { denomination ->
                val count = denominationCounts[denomination] ?: 0
                Box(modifier = Modifier.weight(1f)) {
                    DenominationButton(
                        denomination = denomination,
                        count = count,
                        onClick = { onDenominationPressed(denomination) }
                    )
                }
            }
            // Two empty spacers to keep alignment with 4-column layout
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        // Limpiar button
        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Limpiar",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DenominationButton(
    denomination: Int,
    count: Int,
    onClick: () -> Unit
) {
    Box(
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
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.size(width = 80.dp, height = 48.dp)
            ) {
                Text(
                    text = "$$denomination",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
