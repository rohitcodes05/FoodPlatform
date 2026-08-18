package com.foodplatform.app.data.remote

data class AddressDto(
    val id: String,
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class CreateAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class UpdateAddressRequest(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)
