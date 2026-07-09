package com.example.puntodeventa.data.model

data class MenuItem(
    val id: String,     // UUID — never changes after creation
    val emoji: String,  // e.g. "🌮"
    val name: String    // e.g. "TACOS BLANCA"
)
