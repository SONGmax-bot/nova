package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.ProductDto
import com.example.ui.components.NovaBottomNavigation
import com.example.ui.components.NovaScreen
import com.example.ui.components.NovaTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

enum class SubScreen {
    NONE,
    PRODUCT_DETAIL,
    SETTINGS,
    ORDERS,
    AUTH
}

@Composable
fun NovaMainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentMainScreen by remember { mutableStateOf(NovaScreen.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }

    val cartCount by viewModel.totalCartCount.collectAsState()
    val health by viewModel.healthStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to ViewModel UI events (snackbars)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg.text)
        }
    }

    // Handle Back Press when in a subscreen
    BackHandler(enabled = currentSubScreen != SubScreen.NONE || currentMainScreen != NovaScreen.HOME) {
        if (currentSubScreen != SubScreen.NONE) {
            currentSubScreen = SubScreen.NONE
        } else if (currentMainScreen != NovaScreen.HOME) {
            currentMainScreen = NovaScreen.HOME
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentSubScreen == SubScreen.NONE) {
                NovaTopBar(
                    title = "متجر نوفا | NOVA STORE",
                    health = health,
                    onOpenSettings = { currentSubScreen = SubScreen.SETTINGS }
                )
            }
        },
        bottomBar = {
            if (currentSubScreen == SubScreen.NONE) {
                NovaBottomNavigation(
                    currentScreen = currentMainScreen,
                    cartCount = cartCount ?: 0,
                    onNavigate = { screen ->
                        currentMainScreen = screen
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSubScreen) {
                SubScreen.PRODUCT_DETAIL -> {
                    selectedProduct?.let { product ->
                        ProductDetailScreen(
                            product = product,
                            viewModel = viewModel,
                            onBack = { currentSubScreen = SubScreen.NONE }
                        )
                    }
                }
                SubScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = SubScreen.NONE }
                    )
                }
                SubScreen.ORDERS -> {
                    OrdersScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = SubScreen.NONE }
                    )
                }
                SubScreen.AUTH -> {
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = { currentSubScreen = SubScreen.NONE }
                    )
                }
                SubScreen.NONE -> {
                    when (currentMainScreen) {
                        NovaScreen.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onProductClick = { product ->
                                    selectedProduct = product
                                    currentSubScreen = SubScreen.PRODUCT_DETAIL
                                }
                            )
                        }
                        NovaScreen.CATEGORIES -> {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onCategorySelected = {
                                    currentMainScreen = NovaScreen.HOME
                                }
                            )
                        }
                        NovaScreen.CART -> {
                            CartScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { currentMainScreen = NovaScreen.HOME },
                                onNavigateToAuth = { currentSubScreen = SubScreen.AUTH }
                            )
                        }
                        NovaScreen.ORDERS -> {
                            OrdersScreen(
                                viewModel = viewModel,
                                onBack = { currentMainScreen = NovaScreen.HOME }
                            )
                        }
                        NovaScreen.ACCOUNT -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateToAuth = { currentSubScreen = SubScreen.AUTH },
                                onNavigateToOrders = { currentSubScreen = SubScreen.ORDERS },
                                onNavigateToSettings = { currentSubScreen = SubScreen.SETTINGS }
                            )
                        }
                        NovaScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentMainScreen = NovaScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
