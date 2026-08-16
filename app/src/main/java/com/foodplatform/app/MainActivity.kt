package com.foodplatform.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.foodplatform.app.data.local.SecureTokenStorageImpl
import com.foodplatform.app.data.remote.NetworkModule
import com.foodplatform.app.data.repository.AuthRepository
import com.foodplatform.app.data.repository.SessionManager
import com.foodplatform.app.ui.auth.AuthViewModel
import com.foodplatform.app.ui.auth.AuthViewModelFactory
import com.foodplatform.app.ui.navigation.AppNavigation
import com.foodplatform.app.ui.theme.FoodPlatformTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStorage = SecureTokenStorageImpl(applicationContext)
        val retrofit = NetworkModule.provideRetrofit(
            tokenStorage = tokenStorage,
            onUnauthorized = {
                SessionManager.setUnauthenticated()
            }
        )
        val authApi = NetworkModule.provideAuthApi(retrofit)
        val authRepository = AuthRepository(authApi, tokenStorage)

        val authViewModel: AuthViewModel by viewModels {
            AuthViewModelFactory(authRepository)
        }

        enableEdgeToEdge()
        setContent {
            FoodPlatformTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation(authViewModel = authViewModel)
                }
            }
        }
    }
}
