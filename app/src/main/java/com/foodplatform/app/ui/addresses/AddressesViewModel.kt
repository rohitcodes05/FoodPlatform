package com.foodplatform.app.ui.addresses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodplatform.app.data.remote.AddressDto
import com.foodplatform.app.data.repository.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddressesUiState {
    object Loading : AddressesUiState()
    data class Success(val addresses: List<AddressDto>) : AddressesUiState()
    data class Error(val message: String) : AddressesUiState()
}

class AddressesViewModel(
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddressesUiState>(AddressesUiState.Loading)
    val uiState: StateFlow<AddressesUiState> = _uiState.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _messageEvent = MutableStateFlow<String?>(null)
    val messageEvent: StateFlow<String?> = _messageEvent.asStateFlow()

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = AddressesUiState.Loading
            val result = addressRepository.getAddresses()
            if (result.isSuccess) {
                _uiState.value = AddressesUiState.Success(result.getOrDefault(emptyList()))
            } else {
                _uiState.value = AddressesUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load addresses")
            }
        }
    }

    fun addAddress(street: String, city: String, state: String, postalCode: String, country: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = addressRepository.createAddress(street, city, state, postalCode, country)
            _isActionLoading.value = false

            if (result.isSuccess) {
                _messageEvent.value = "Address added successfully"
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
                _messageEvent.value = "Address updated successfully"
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
                _messageEvent.value = "Address deleted successfully"
                loadAddresses()
            } else {
                _errorEvent.value = result.exceptionOrNull()?.message ?: "Failed to delete address"
            }
        }
    }

    fun clearError() {
        _errorEvent.value = null
    }

    fun clearMessage() {
        _messageEvent.value = null
    }
}

class AddressesViewModelFactory(
    private val addressRepository: AddressRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddressesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddressesViewModel(addressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
