package com.foodplatform.app.ui.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.CreatePartnerProductRequest
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.PartnerProductDto
import com.foodplatform.app.data.remote.UpdatePartnerProductRequest
import com.foodplatform.app.data.repository.PartnerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI State ───────────────────────────────────────────────────────────────────

sealed class PartnerOrdersUiState {
    object Loading : PartnerOrdersUiState()
    data class Success(val orders: List<OrderDto>) : PartnerOrdersUiState()
    data class Error(val message: String) : PartnerOrdersUiState()
}

sealed class PartnerProductsUiState {
    object Loading : PartnerProductsUiState()
    data class Success(val products: List<PartnerProductDto>) : PartnerProductsUiState()
    data class Error(val message: String) : PartnerProductsUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PartnerDashboardViewModel(
    private val partnerRepository: PartnerRepository
) : ViewModel() {

    // Orders state
    private val _ordersUiState = MutableStateFlow<PartnerOrdersUiState>(PartnerOrdersUiState.Loading)
    val ordersUiState: StateFlow<PartnerOrdersUiState> = _ordersUiState.asStateFlow()

    // Products state
    private val _productsUiState = MutableStateFlow<PartnerProductsUiState>(PartnerProductsUiState.Loading)
    val productsUiState: StateFlow<PartnerProductsUiState> = _productsUiState.asStateFlow()

    // Mutation state (shared for orders and products mutations)
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        loadOrders()
        loadProducts()
    }

    // ── Orders ─────────────────────────────────────────────────────────────────

    fun loadOrders() {
        _ordersUiState.value = PartnerOrdersUiState.Loading
        viewModelScope.launch {
            partnerRepository.getPartnerOrders()
                .onSuccess { _ordersUiState.value = PartnerOrdersUiState.Success(it) }
                .onFailure { _ordersUiState.value = PartnerOrdersUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        _isUpdating.value = true
        _actionError.value = null
        viewModelScope.launch {
            partnerRepository.updatePartnerOrderStatus(orderId, status)
                .onSuccess {
                    _isUpdating.value = false
                    loadOrders()
                }
                .onFailure {
                    _isUpdating.value = false
                    _actionError.value = it.message
                }
        }
    }

    // ── Products ───────────────────────────────────────────────────────────────

    fun loadProducts() {
        _productsUiState.value = PartnerProductsUiState.Loading
        viewModelScope.launch {
            partnerRepository.getPartnerProducts()
                .onSuccess { _productsUiState.value = PartnerProductsUiState.Success(it) }
                .onFailure { _productsUiState.value = PartnerProductsUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun createProduct(name: String, type: String, price: Double, description: String?) {
        _isUpdating.value = true
        _actionError.value = null
        // NOTE: partnerId is NOT included — backend derives it from JWT
        val request = CreatePartnerProductRequest(
            name = name,
            description = description?.takeIf { it.isNotBlank() },
            type = type,
            price = price
        )
        viewModelScope.launch {
            partnerRepository.createPartnerProduct(request)
                .onSuccess {
                    _isUpdating.value = false
                    loadProducts()
                }
                .onFailure {
                    _isUpdating.value = false
                    _actionError.value = it.message
                }
        }
    }

    fun toggleProductAvailability(product: PartnerProductDto) {
        _isUpdating.value = true
        _actionError.value = null
        // Send only isAvailable — no partnerId in request
        val request = UpdatePartnerProductRequest(isAvailable = !product.isAvailable)
        viewModelScope.launch {
            partnerRepository.updatePartnerProduct(product.id, request)
                .onSuccess {
                    _isUpdating.value = false
                    loadProducts()
                }
                .onFailure {
                    _isUpdating.value = false
                    _actionError.value = it.message
                }
        }
    }

    fun deleteProduct(productId: String) {
        _isUpdating.value = true
        _actionError.value = null
        viewModelScope.launch {
            partnerRepository.deletePartnerProduct(productId)
                .onSuccess {
                    _isUpdating.value = false
                    loadProducts()
                }
                .onFailure {
                    _isUpdating.value = false
                    _actionError.value = it.message // 409 shows user-friendly message
                }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

class PartnerDashboardViewModelFactory(
    private val partnerRepository: PartnerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PartnerDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PartnerDashboardViewModel(partnerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
