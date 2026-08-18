package com.foodplatform.app.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.repository.AddressRepository
import com.foodplatform.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val addressRepository: AddressRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _orderSuccessEvent = MutableStateFlow<Boolean>(false)
    val orderSuccessEvent: StateFlow<Boolean> = _orderSuccessEvent.asStateFlow()

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Loading
            val result = addressRepository.getAddresses()
            if (result.isSuccess) {
                val addresses = result.getOrDefault(emptyList())
                val defaultSelected = addresses.firstOrNull()?.id
                _uiState.value = CheckoutUiState.Success(addresses, defaultSelected)
            } else {
                _uiState.value = CheckoutUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load addresses")
            }
        }
    }

    fun selectAddress(addressId: String) {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            _uiState.value = currentState.copy(selectedAddressId = addressId)
        }
    }

    fun placeOrder() {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Success) return
        val addressId = currentState.selectedAddressId
        if (addressId == null) {
            _errorEvent.value = "Please select an address"
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = orderRepository.createOrder(addressId)
            _isActionLoading.value = false
            
            if (result.isSuccess) {
                _orderSuccessEvent.value = true
            } else {
                _errorEvent.value = result.exceptionOrNull()?.message ?: "Failed to place order"
            }
        }
    }

    fun addAddress(street: String, city: String, state: String, postalCode: String, country: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = addressRepository.createAddress(street, city, state, postalCode, country)
            _isActionLoading.value = false

            if (result.isSuccess) {
                loadAddresses()
            } else {
                _errorEvent.value = result.exceptionOrNull()?.message ?: "Failed to add address"
            }
        }
    }

    fun updateAddress(id: String, street: String, city: String, state: String, postalCode: String, country: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = addressRepository.updateAddress(id, street, city, state, postalCode, country)
            _isActionLoading.value = false

            if (result.isSuccess) {
                loadAddresses()
            } else {
                _errorEvent.value = result.exceptionOrNull()?.message ?: "Failed to update address"
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = addressRepository.deleteAddress(id)
            _isActionLoading.value = false

            if (result.isSuccess) {
                val currentState = _uiState.value
                if (currentState is CheckoutUiState.Success && currentState.selectedAddressId == id) {
                    _uiState.value = currentState.copy(selectedAddressId = null)
                }
                loadAddresses()
            } else {
                _errorEvent.value = result.exceptionOrNull()?.message ?: "Failed to delete address"
            }
        }
    }

    fun clearError() {
        _errorEvent.value = null
    }

    fun clearSuccessEvent() {
        _orderSuccessEvent.value = false
    }
}

class CheckoutViewModelFactory(
    private val addressRepository: AddressRepository,
    private val orderRepository: OrderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CheckoutViewModel(addressRepository, orderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
