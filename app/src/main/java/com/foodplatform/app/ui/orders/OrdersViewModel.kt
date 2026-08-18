package com.foodplatform.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _detailsUiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val detailsUiState: StateFlow<OrderDetailsUiState> = _detailsUiState.asStateFlow()

    fun loadOrders() {
        _uiState.value = OrdersUiState.Loading
        viewModelScope.launch {
            val result = orderRepository.getOrders()
            result.fold(
                onSuccess = { orders ->
                    _uiState.value = OrdersUiState.Success(orders)
                },
                onFailure = { error ->
                    _uiState.value = OrdersUiState.Error(error.message ?: "Failed to load orders")
                }
            )
        }
    }

    fun loadOrderDetails(orderId: String) {
        _detailsUiState.value = OrderDetailsUiState.Loading
        viewModelScope.launch {
            val result = orderRepository.getOrderById(orderId)
            result.fold(
                onSuccess = { order ->
                    _detailsUiState.value = OrderDetailsUiState.Success(order)
                },
                onFailure = { error ->
                    _detailsUiState.value = OrderDetailsUiState.Error(error.message ?: "Failed to load order details")
                }
            )
        }
    }
}

class OrdersViewModelFactory(private val orderRepository: OrderRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrdersViewModel(orderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
