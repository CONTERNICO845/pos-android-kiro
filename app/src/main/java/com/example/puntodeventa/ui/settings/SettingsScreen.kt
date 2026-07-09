package com.example.puntodeventa.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.ui.theme.BackgroundPrimary
import com.example.puntodeventa.ui.theme.CardText

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "CONFIGURACIÓN",
            color      = CardText,
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp
        )
    }
}
