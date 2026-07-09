package com.example.puntodeventa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.ProductRepository
import com.example.puntodeventa.ui.configuration.ConfigurationScreen
import com.example.puntodeventa.ui.configuration.ConfigurationViewModel
import com.example.puntodeventa.ui.home.HomeScreen
import com.example.puntodeventa.ui.home.HomeViewModel
import com.example.puntodeventa.ui.navigation.AppNavRail
import com.example.puntodeventa.ui.navigation.NavDestination
import com.example.puntodeventa.ui.newproduct.NewProductViewModel
import com.example.puntodeventa.ui.printer.PrinterScreen
import com.example.puntodeventa.ui.stats.StatsScreen
import com.example.puntodeventa.ui.theme.BackgroundPrimary
import com.example.puntodeventa.ui.theme.PuntoDeVentaTheme
import com.example.puntodeventa.ui.tickets.TicketsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Build the data layer once per Activity lifetime, outside Compose scope,
        // so it is not recreated on recomposition.
        val db = AppDatabase.getInstance(this)
        val menuRepository = MenuRepository(db.menuItemDao())
        val categoryRepo = CategoryRepository(db.categoryDao())
        val productRepo = ProductRepository(
            productDao = db.productDao(),
            groupDao   = db.customizationGroupDao(),
            optionDao  = db.customizationOptionDao(),
            database   = db
        )

        setContent {
            PuntoDeVentaTheme {
                // Hoist HomeViewModel to MainActivity scope so menuId is available here
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(menuRepository)
                )
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                // Derive the active menuId from the first menu item (Phase 2 stub)
                val activeMenuId = homeUiState.menuItems.firstOrNull()?.id ?: ""

                var currentDestination: NavDestination by remember {
                    mutableStateOf(NavDestination.Home)
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundPrimary)
                ) {
                    // Persistent left navigation rail — must remain first child
                    AppNavRail(
                        currentDestination    = currentDestination,
                        onDestinationSelected = { currentDestination = it }
                    )

                    // Main content area
                    when (currentDestination) {
                        NavDestination.Home -> HomeScreen(viewModel = homeViewModel)
                        NavDestination.Settings -> ConfigurationScreen(
                            viewModel = viewModel(
                                factory = ConfigurationViewModel.Factory(
                                    categoryRepository = categoryRepo,
                                    productRepository  = productRepo,
                                    menuId             = activeMenuId
                                )
                            ),
                            newProductViewModel = viewModel(
                                factory = NewProductViewModel.Factory(
                                    productRepository  = productRepo,
                                    categoryRepository = categoryRepo,
                                    menuRepository     = menuRepository,
                                    database           = db
                                )
                            )
                        )
                        NavDestination.Stats    -> StatsScreen()
                        NavDestination.Tickets  -> TicketsScreen()
                        NavDestination.Printer  -> PrinterScreen()
                    }
                }
            }
        }
    }
}
