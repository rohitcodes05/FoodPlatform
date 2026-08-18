package com.foodplatform.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.repository.CartRepository
import com.foodplatform.app.data.repository.ProductRepository
import com.foodplatform.app.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProductDetailUiState {
    object Loading : ProductDetailUiState()
    data class Success(val product: ProductDto) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}

class ProductDetailViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _addToCartEvent = MutableStateFlow<String?>(null)
    val addToCartEvent: StateFlow<String?> = _addToCartEvent.asStateFlow()

    private val _isAddingToCart = MutableStateFlow(false)
    val isAddingToCart: StateFlow<Boolean> = _isAddingToCart.asStateFlow()

    fun loadProduct(id: String) {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            when (val result = productRepository.getProductById(id)) {
                is Resource.Success -> {
                    _uiState.value = ProductDetailUiState.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = ProductDetailUiState.Error(result.message)
                }
            }
        }
    }

    fun retry(productId: String) {
        if (_uiState.value is ProductDetailUiState.Error) {
            loadProduct(productId)
        }
    }

    fun addToCart(productId: String, quantity: Int) {
        if (_isAddingToCart.value) return
        _isAddingToCart.value = true

        viewModelScope.launch {
            when (val result = cartRepository.addItem(productId, quantity)) {
                is Resource.Success<*> -> {
                    _addToCartEvent.value = "Success"
                }
                is Resource.Error -> {
                    _addToCartEvent.value = result.message
                }
            }
            _isAddingToCart.value = false
        }
    }

    fun clearAddToCartEvent() {
        _addToCartEvent.value = null
    }
}

class ProductDetailViewModelFactory(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductDetailViewModel(productRepository, cartRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
