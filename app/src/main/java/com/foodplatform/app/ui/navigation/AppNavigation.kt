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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Welcome to FoodPlatform!")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { authViewModel.logout() }) {
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}
