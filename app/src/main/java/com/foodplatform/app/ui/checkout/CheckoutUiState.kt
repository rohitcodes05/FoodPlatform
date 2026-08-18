package com.foodplatform.app.ui.checkout

import com.foodplatform.app.data.remote.AddressDto

sealed class CheckoutUiState {
    object Loading : CheckoutUiState()
    data class Success(
        val addresses: List<AddressDto>,
        val selectedAddressId: String? = null
    ) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}
