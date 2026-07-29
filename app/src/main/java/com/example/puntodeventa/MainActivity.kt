package com.example.puntodeventa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puntodeventa.data.local.AppDatabase
import com.example.puntodeventa.data.repository.CategoryRepository
import com.example.puntodeventa.data.repository.CatalogJsonRepository
import com.example.puntodeventa.data.repository.MenuRepository
import com.example.puntodeventa.data.repository.OrderRepository
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import com.example.puntodeventa.data.repository.ProductRepository
import com.example.puntodeventa.ui.configuration.ConfigurationScreen
import com.example.puntodeventa.ui.configuration.ConfigurationViewModel
import com.example.puntodeventa.ui.home.HomeScreen
import com.example.puntodeventa.ui.home.HomeViewModel
import com.example.puntodeventa.ui.navigation.AppNavRail
import com.example.puntodeventa.ui.navigation.NavDestination
import com.example.puntodeventa.ui.newproduct.NewProductViewModel
import com.example.puntodeventa.ui.pos.PosScreen
import com.example.puntodeventa.ui.pos.PosViewModel
import com.example.puntodeventa.ui.printer.PrinterConfigViewModel
import com.example.puntodeventa.ui.printer.PrinterScreen
import com.example.puntodeventa.ui.stats.StatsScreen
import com.example.puntodeventa.ui.stats.StatsViewModel
import com.example.puntodeventa.data.repository.ThemePreferencesRepository
import com.example.puntodeventa.data.repository.themeDataStore
import com.example.puntodeventa.ui.theme.PuntoDeVentaTheme
import com.example.puntodeventa.ui.theme.ThemeSelectorScreen
import com.example.puntodeventa.ui.theme.ThemeViewModel
import androidx.compose.material3.MaterialTheme
import com.example.puntodeventa.ui.tickets.TicketHistoryScreen
import com.example.puntodeventa.ui.tickets.TicketHistoryViewModel

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
        val printerPrefsRepo = PrinterPreferencesRepository(this)
        val orderRepo = OrderRepository(db.orderDao(), db)
        val themePrefsRepo = ThemePreferencesRepository(themeDataStore)
        val catalogJsonRepo = CatalogJsonRepository(
            database   = db,
            menuItemDao = db.menuItemDao(),
            categoryDao = db.categoryDao(),
            productDao  = db.productDao(),
            groupDao    = db.customizationGroupDao(),
            optionDao   = db.customizationOptionDao()
        )

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.Factory(themePrefsRepo)
            )
            val currentTheme by themeViewModel.currentTheme.collectAsStateWithLifecycle()

            PuntoDeVentaTheme(appTheme = currentTheme) {
                // Hoist HomeViewModel to MainActivity scope so menuId is available here
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(menuRepository)
                )
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                // Mutable active menu ID — updated when user taps a MenuItemCard
                var activeMenuId by remember {
                    mutableStateOf("")
                }

                // Initialize activeMenuId from the first menu item when available
                LaunchedEffect(homeUiState.menuItems) {
                    if (activeMenuId.isEmpty() && homeUiState.menuItems.isNotEmpty()) {
                        activeMenuId = homeUiState.menuItems.first().id
                    }
                }

                var currentDestination: NavDestination by remember {
                    mutableStateOf(NavDestination.Home)
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Persistent left navigation rail — must remain first child
                    AppNavRail(
                        currentDestination    = currentDestination,
                        onDestinationSelected = { currentDestination = it }
                    )

                    // Main content area
                    when (currentDestination) {
                        NavDestination.Home -> HomeScreen(
                            onNavigateToPOS = { menuId ->
                                activeMenuId = menuId
                                currentDestination = NavDestination.Pos
                            },
                            viewModel = homeViewModel
                        )
                        NavDestination.Pos -> {
                            val posViewModel: PosViewModel = viewModel(
                                factory = PosViewModel.Factory(
                                    categoryRepository          = categoryRepo,
                                    productRepository           = productRepo,
                                    orderRepository             = orderRepo,
                                    menuId                      = activeMenuId,
                                    printerPreferencesRepository = printerPrefsRepo
                                )
                            )

                            // Apply the active menu filter whenever it changes
                            LaunchedEffect(activeMenuId) {
                                posViewModel.selectMenu(activeMenuId)
                            }

                            PosScreen(
                                viewModel              = posViewModel,
                                customizationGroupDao  = db.customizationGroupDao(),
                                customizationOptionDao = db.customizationOptionDao(),
                                menuItems              = homeUiState.menuItems
                            )
                        }
                        NavDestination.Settings -> ConfigurationScreen(
                            viewModel = viewModel(
                                factory = ConfigurationViewModel.Factory(
                                    categoryRepository = categoryRepo,
                                    productRepository  = productRepo,
                                    catalogJsonRepository = catalogJsonRepo,
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
                        NavDestination.Stats    -> {
                            val statsViewModel: StatsViewModel = viewModel(
                                factory = StatsViewModel.Factory(orderRepo)
                            )
                            val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()
                            StatsScreen(
                                uiState = statsUiState,
                                onFilterChange = statsViewModel::onFilterChange,
                                onDateRangeSelected = statsViewModel::onDateRangeSelected,
                                onDateRangePickerDismissed = statsViewModel::onDateRangePickerDismissed
                            )
                        }
                        NavDestination.Tickets  -> {
                            val ticketHistoryViewModel: TicketHistoryViewModel = viewModel(
                                factory = TicketHistoryViewModel.Factory(orderRepo, printerPrefsRepo)
                            )
                            val ticketHistoryUiState by ticketHistoryViewModel.uiState.collectAsStateWithLifecycle()
                            TicketHistoryScreen(
                                uiState = ticketHistoryUiState,
                                onFilterChange = ticketHistoryViewModel::onFilterChange,
                                onReprintTicket = ticketHistoryViewModel::onReprintTicket,
                                onDateRangeSelected = ticketHistoryViewModel::onDateRangeSelected,
                                onDateRangePickerDismissed = ticketHistoryViewModel::onDateRangePickerDismissed
                            )
                        }
                        NavDestination.Printer  -> PrinterScreen(
                            viewModel = viewModel(
                                factory = PrinterConfigViewModel.Factory(printerPrefsRepo)
                            )
                        )
                        NavDestination.Appearance -> ThemeSelectorScreen(
                            currentTheme = currentTheme,
                            onThemeSelected = themeViewModel::selectTheme
                        )
                    }
                }
            }
        }
    }
}
