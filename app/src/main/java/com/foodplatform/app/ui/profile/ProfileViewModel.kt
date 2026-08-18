package com.foodplatform.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.repository.AuthRepository
import com.foodplatform.app.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    fun loadProfile() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.getCurrentUser()) {
                is AuthResult.Success -> {
                    _uiState.value = ProfileUiState.Success(result.data)
                }
                is AuthResult.Error -> {
                    _uiState.value = ProfileUiState.Error(result.message)
                }
            }
        }
    }

    fun updateProfile(name: String, phone: String?) {
        _isUpdating.value = true
        _updateError.value = null
        viewModelScope.launch {
            when (val result = authRepository.updateProfile(name, phone)) {
                is AuthResult.Success -> {
                    _isUpdating.value = false
                    _uiState.value = ProfileUiState.Success(result.data)
                }
                is AuthResult.Error -> {
                    _isUpdating.value = false
                    _updateError.value = result.message
                }
            }
        }
    }

    fun clearUpdateError() {
        _updateError.value = null
    }

    fun logout() {
        authRepository.logout()
        _logoutEvent.value = true
    }

    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }
}

class ProfileViewModelFactory(private val authRepository: AuthRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
