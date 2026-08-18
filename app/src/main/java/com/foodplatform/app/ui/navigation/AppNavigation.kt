package com.foodplatform.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import com.foodplatform.app.data.repository.SessionManager
import com.foodplatform.app.data.repository.SessionState
import com.foodplatform.app.ui.auth.AuthViewModel
import com.foodplatform.app.ui.auth.LoginScreen
import com.foodplatform.app.ui.auth.RegisterScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val sessionState by SessionManager.sessionState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkSession()
    }

    when (sessionState) {
        SessionState.UNKNOWN -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SessionState.UNAUTHENTICATED -> {
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToRegister = { navController.navigate("register") }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }
            }
        }
        SessionState.AUTHENTICATED -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofit = remember {
                com.foodplatform.app.data.remote.NetworkModule.provideRetrofit(
                    com.foodplatform.app.data.local.SecureTokenStorageImpl(context),
                    onUnauthorized = { SessionManager.setUnauthenticated() }
                )
            }

            val productRepository = remember {
                com.foodplatform.app.data.repository.ProductRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideProductApi(retrofit)
                )
            }

            val cartRepository = remember {
                com.foodplatform.app.data.repository.CartRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideCartApi(retrofit)
                )
            }

            NavHost(navController = navController, startDestination = "auth_flow") {
                navigation(startDestination = "catalog", route = "auth_flow") {
                    composable("catalog") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val cartViewModel: com.foodplatform.app.ui.cart.CartViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.cart.CartViewModelFactory(cartRepository)
                        )

                        val cartUiState by cartViewModel.uiState.collectAsState()
                        val cartItemCount = if (cartUiState is com.foodplatform.app.ui.cart.CartUiState.Success) {
                            (cartUiState as com.foodplatform.app.ui.cart.CartUiState.Success).cart.items.sumOf { it.quantity.toInt() }
                        } else 0

                        val catalogViewModel: com.foodplatform.app.ui.catalog.CatalogViewModel = viewModel(
                            factory = com.foodplatform.app.ui.catalog.CatalogViewModelFactory(productRepository)
                        )

                        com.foodplatform.app.ui.catalog.CatalogScreen(
                            viewModel = catalogViewModel,
                            cartItemCount = cartItemCount,
                            onNavigateToProduct = { productId -> navController.navigate("product_detail/$productId") },
                            onNavigateToCart = { navController.navigate("cart") },
                            onLogout = { authViewModel.logout() }
                        )
                    }

                    composable("product_detail/{productId}") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val cartViewModel: com.foodplatform.app.ui.cart.CartViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.cart.CartViewModelFactory(cartRepository)
                        )

                        val cartUiState by cartViewModel.uiState.collectAsState()
                        val cartItemCount = if (cartUiState is com.foodplatform.app.ui.cart.CartUiState.Success) {
                            (cartUiState as com.foodplatform.app.ui.cart.CartUiState.Success).cart.items.sumOf { it.quantity.toInt() }
                        } else 0

                        val productId = backStackEntry.arguments?.getString("productId") ?: return@composable

                        val detailViewModel: com.foodplatform.app.ui.catalog.ProductDetailViewModel = viewModel(
                            factory = com.foodplatform.app.ui.catalog.ProductDetailViewModelFactory(productRepository, cartRepository)
                        )

                        com.foodplatform.app.ui.catalog.ProductDetailScreen(
                            productId = productId,
                            viewModel = detailViewModel,
                            cartItemCount = cartItemCount,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCart = { navController.navigate("cart") }
                        )
                    }

                    composable("cart") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val cartViewModel: com.foodplatform.app.ui.cart.CartViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.cart.CartViewModelFactory(cartRepository)
                        )
                        val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

                        com.foodplatform.app.ui.cart.CartScreen(
                            viewModel = cartViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }
}
