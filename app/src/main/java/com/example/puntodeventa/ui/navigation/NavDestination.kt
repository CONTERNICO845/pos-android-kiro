package com.example.puntodeventa.ui.navigation

sealed class NavDestination(val route: String, val label: String) {
    object Home       : NavDestination("home",       "Inicio")
    object Pos        : NavDestination("pos",        "POS")
    object Stats      : NavDestination("stats",      "Estadísticas")
    object Settings   : NavDestination("settings",   "Configuración")
    object Tickets    : NavDestination("tickets",    "Tickets")
    object Printer    : NavDestination("printer",    "Impresora")
    object Appearance : NavDestination("appearance", "Apariencia")
}
