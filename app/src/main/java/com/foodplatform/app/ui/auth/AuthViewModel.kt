package com.foodplatform.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.LoginRequest
import com.foodplatform.app.data.remote.RegisterRequest
import com.foodplatform.app.data.repository.AuthRepository
import com.foodplatform.app.data.repository.AuthResult
import com.foodplatform.app.data.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun checkSession() {
        if (!repository.hasToken()) {
            SessionManager.setUnauthenticated()
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.getCurrentUser()) {
                is AuthResult.Success -> {
                    SessionManager.setAuthenticated()
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    // Only log out if it's explicitly a session error, 
                    // AuthInterceptor handles 401 clearing. 
                    // If network fails, don't clear token instantly. 
                    // Let interceptor handle actual 401s.
                    if (result.message.contains("401") || !repository.hasToken()) {
                        SessionManager.setUnauthenticated()
                    }
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.login(request)) {
                is AuthResult.Success -> {
                    SessionManager.setAuthenticated()
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.registerAndLogin(request)) {
                is AuthResult.Success -> {
                    SessionManager.setAuthenticated()
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }
    
    fun logout() {
        repository.logout()
        SessionManager.setUnauthenticated()
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
