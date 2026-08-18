package com.foodplatform.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.repository.OrderRepository
import com.foodplatform.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val orderRepository: OrderRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _detailsUiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val detailsUiState: StateFlow<OrderDetailsUiState> = _detailsUiState.asStateFlow()

    private val _reviewSubmitState = MutableStateFlow<Result<Unit>?>(null)
    val reviewSubmitState: StateFlow<Result<Unit>?> = _reviewSubmitState.asStateFlow()

    fun clearReviewState() {
        _reviewSubmitState.value = null
    }

    fun submitReview(orderItemId: String, rating: Int, comment: String?, orderId: String) {
        viewModelScope.launch {
            val result = reviewRepository.createReview(orderItemId, rating, comment)
            _reviewSubmitState.value = result
            if (result.isSuccess) {
                loadOrderDetails(orderId)
            }
        }
    }

    fun updateReview(reviewId: String, rating: Int, comment: String?, orderId: String) {
        viewModelScope.launch {
            val result = reviewRepository.updateReview(reviewId, rating, comment)
            _reviewSubmitState.value = result
            if (result.isSuccess) {
                loadOrderDetails(orderId)
            }
        }
    }

    fun deleteReview(reviewId: String, orderId: String) {
        viewModelScope.launch {
            val result = reviewRepository.deleteReview(reviewId)
            _reviewSubmitState.value = result
            if (result.isSuccess) {
                loadOrderDetails(orderId)
            }
        }
    }

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

class OrdersViewModelFactory(
    private val orderRepository: OrderRepository,
    private val reviewRepository: ReviewRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrdersViewModel(orderRepository, reviewRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
