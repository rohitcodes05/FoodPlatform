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

            val categoryRepository = remember {
                com.foodplatform.app.data.repository.CategoryRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideCategoryApi(retrofit)
                )
            }

            val reviewRepository = remember {
                com.foodplatform.app.data.repository.ReviewRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideReviewApi(retrofit)
                )
            }

            val cartRepository = remember {
                com.foodplatform.app.data.repository.CartRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideCartApi(retrofit)
                )
            }

            val addressRepository = remember {
                com.foodplatform.app.data.repository.AddressRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideAddressApi(retrofit)
                )
            }

            val orderRepository = remember {
                com.foodplatform.app.data.repository.OrderRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideOrderApi(retrofit)
                )
            }

            val authRepository = remember {
                com.foodplatform.app.data.repository.AuthRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideAuthApi(retrofit),
                    com.foodplatform.app.data.local.SecureTokenStorageImpl(context)
                )
            }

            val adminRepository = remember {
                com.foodplatform.app.data.repository.AdminRepository(
                    com.foodplatform.app.data.remote.NetworkModule.provideAdminApi(retrofit)
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
                            factory = com.foodplatform.app.ui.catalog.CatalogViewModelFactory(productRepository, categoryRepository)
                        )

                        com.foodplatform.app.ui.catalog.CatalogScreen(
                            viewModel = catalogViewModel,
                            cartItemCount = cartItemCount,
                            onNavigateToProduct = { productId -> navController.navigate("product_detail/$productId") },
                            onNavigateToCart = { navController.navigate("cart") },
                            onNavigateToProfile = { navController.navigate("profile") },
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
                            factory = com.foodplatform.app.ui.catalog.ProductDetailViewModelFactory(productRepository, cartRepository, reviewRepository)
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
                            onNavigateToCheckout = { navController.navigate("checkout") },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    composable("checkout") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val cartViewModel: com.foodplatform.app.ui.cart.CartViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.cart.CartViewModelFactory(cartRepository)
                        )
                        val cartUiState by cartViewModel.uiState.collectAsState()
                        val cartItemCount = if (cartUiState is com.foodplatform.app.ui.cart.CartUiState.Success) {
                            (cartUiState as com.foodplatform.app.ui.cart.CartUiState.Success).cart.items.sumOf { it.quantity.toInt() }
                        } else 0
                        val cartTotalAmount = if (cartUiState is com.foodplatform.app.ui.cart.CartUiState.Success) {
                            (cartUiState as com.foodplatform.app.ui.cart.CartUiState.Success).cart.items.sumOf { it.quantity.toInt() * it.product.price.toDouble() }.toString()
                        } else "0"

                        val checkoutViewModel: com.foodplatform.app.ui.checkout.CheckoutViewModel = viewModel(
                            factory = com.foodplatform.app.ui.checkout.CheckoutViewModelFactory(addressRepository, orderRepository)
                        )
                        val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

                        com.foodplatform.app.ui.checkout.CheckoutScreen(
                            viewModel = checkoutViewModel,
                            cartItemCount = cartItemCount,
                            cartTotalAmount = cartTotalAmount,
                            onNavigateBack = { navController.popBackStack() },
                            onOrderSuccess = {
                                cartViewModel.loadCart() // Force sync to show empty cart
                                navController.popBackStack("catalog", inclusive = false)
                            },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    composable("profile") {
                        val profileViewModel: com.foodplatform.app.ui.profile.ProfileViewModel = viewModel(
                            factory = com.foodplatform.app.ui.profile.ProfileViewModelFactory(authRepository)
                        )
                        com.foodplatform.app.ui.profile.ProfileScreen(
                            viewModel = profileViewModel,
                            onNavigateToOrderHistory = { navController.navigate("order_history") },
                            onNavigateToAddresses = { navController.navigate("addresses") },
                            onNavigateToAdmin = { navController.navigate("admin_dashboard") },
                            onNavigateToLogin = {
                                SessionManager.setUnauthenticated()
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("addresses") {
                        val addressesViewModel: com.foodplatform.app.ui.addresses.AddressesViewModel = viewModel(
                            factory = com.foodplatform.app.ui.addresses.AddressesViewModelFactory(addressRepository)
                        )
                        com.foodplatform.app.ui.addresses.AddressesScreen(
                            viewModel = addressesViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("order_history") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val ordersViewModel: com.foodplatform.app.ui.orders.OrdersViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.orders.OrdersViewModelFactory(orderRepository, reviewRepository)
                        )
                        com.foodplatform.app.ui.orders.OrderHistoryScreen(
                            viewModel = ordersViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetails = { orderId -> navController.navigate("order_details/$orderId") }
                        )
                    }

                    composable("order_details/{orderId}") { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_flow") }
                        val ordersViewModel: com.foodplatform.app.ui.orders.OrdersViewModel = viewModel(
                            parentEntry,
                            factory = com.foodplatform.app.ui.orders.OrdersViewModelFactory(orderRepository, reviewRepository)
                        )
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                        com.foodplatform.app.ui.orders.OrderDetailsScreen(
                            orderId = orderId,
                            viewModel = ordersViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("admin_dashboard") {
                        val adminViewModel: com.foodplatform.app.ui.admin.AdminDashboardViewModel = viewModel(
                            factory = com.foodplatform.app.ui.admin.AdminDashboardViewModelFactory(adminRepository)
                        )
                        com.foodplatform.app.ui.admin.AdminDashboardScreen(
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
