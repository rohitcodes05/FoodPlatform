package com.foodplatform.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminOrdersUiState {
    object Loading : AdminOrdersUiState()
    data class Success(val orders: List<OrderDto>) : AdminOrdersUiState()
    data class Error(val message: String) : AdminOrdersUiState()
}

class AdminDashboardViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminOrdersUiState>(AdminOrdersUiState.Loading)
    val uiState: StateFlow<AdminOrdersUiState> = _uiState.asStateFlow()
    
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    
    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        _uiState.value = AdminOrdersUiState.Loading
        viewModelScope.launch {
            val result = adminRepository.getOrders()
            result.onSuccess {
                _uiState.value = AdminOrdersUiState.Success(it)
            }.onFailure {
                _uiState.value = AdminOrdersUiState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        _isUpdating.value = true
        _updateError.value = null
        viewModelScope.launch {
            val result = adminRepository.updateOrderStatus(orderId, status)
            result.onSuccess {
                _isUpdating.value = false
                loadOrders() // Refresh
            }.onFailure {
                _isUpdating.value = false
                _updateError.value = it.message
            }
        }
    }

    fun updateDeliveryStatus(deliveryId: String, status: String, trackingCode: String?) {
        _isUpdating.value = true
        _updateError.value = null
        viewModelScope.launch {
            val result = adminRepository.updateDeliveryStatus(deliveryId, status, trackingCode)
            result.onSuccess {
                _isUpdating.value = false
                loadOrders() // Refresh
            }.onFailure {
                _isUpdating.value = false
                _updateError.value = it.message
            }
        }
    }

    fun clearUpdateError() {
        _updateError.value = null
    }
}

class AdminDashboardViewModelFactory(private val adminRepository: AdminRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(adminRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
