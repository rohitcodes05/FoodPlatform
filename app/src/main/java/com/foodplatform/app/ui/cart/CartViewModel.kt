package com.foodplatform.app.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.CartDto
import com.foodplatform.app.data.repository.CartRepository
import com.foodplatform.app.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CartUiState {
    object Loading : CartUiState()
    data class Success(val cart: CartDto) : CartUiState()
    data class Error(val message: String) : CartUiState()
}

class CartViewModel(
    private val repository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    init {
        loadCart()
        viewModelScope.launch {
            repository.cartUpdates.collect {
                loadCart(isRefresh = true)
            }
        }
    }

    fun loadCart(isRefresh: Boolean = false) {
        if (!isRefresh && _uiState.value !is CartUiState.Success) {
            _uiState.value = CartUiState.Loading
        }
        viewModelScope.launch {
            when (val result = repository.getCart()) {
                is Resource.Success -> _uiState.value = CartUiState.Success(result.data)
                is Resource.Error -> {
                    if (!isRefresh) {
                        _uiState.value = CartUiState.Error(result.message)
                    } else {
                        _errorEvent.value = result.message
                    }
                }
            }
        }
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        if (_isActionLoading.value) return
        _isActionLoading.value = true

        viewModelScope.launch {
            when (val result = repository.updateItemQuantity(itemId, quantity)) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                }
                is Resource.Error -> {
                    _errorEvent.value = result.message
                    _isActionLoading.value = false
                }
            }
        }
    }

    fun removeItem(itemId: String) {
        if (_isActionLoading.value) return
        _isActionLoading.value = true

        viewModelScope.launch {
            when (val result = repository.removeItem(itemId)) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                }
                is Resource.Error -> {
                    _errorEvent.value = result.message
                    _isActionLoading.value = false
                }
            }
        }
    }

    fun clearCart() {
        if (_isActionLoading.value) return
        _isActionLoading.value = true

        viewModelScope.launch {
            when (val result = repository.clearCart()) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                }
                is Resource.Error -> {
                    _errorEvent.value = result.message
                    _isActionLoading.value = false
                }
            }
        }
    }



    fun clearError() {
        _errorEvent.value = null
    }

    fun calculateTotal(cart: CartDto): Double {
        return cart.items.sumOf { item ->
            val price = item.weightOption?.priceOverride ?: item.product.price
            item.quantity * price
        }
    }
}

class CartViewModelFactory(private val repository: CartRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
