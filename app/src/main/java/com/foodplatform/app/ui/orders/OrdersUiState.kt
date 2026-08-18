package com.foodplatform.app.ui.orders

import com.foodplatform.app.data.remote.OrderDto

sealed class OrdersUiState {
    object Loading : OrdersUiState()
    data class Success(val orders: List<OrderDto>) : OrdersUiState()
    data class Error(val message: String) : OrdersUiState()
}

sealed class OrderDetailsUiState {
    object Loading : OrderDetailsUiState()
    data class Success(val order: OrderDto) : OrderDetailsUiState()
    data class Error(val message: String) : OrderDetailsUiState()
}
