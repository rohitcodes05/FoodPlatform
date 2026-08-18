package com.foodplatform.app.data.remote

data class CreateOrderRequest(
    val addressId: String
)

data class OrderDto(
    val id: String,
    val userId: String,
    val totalAmount: String,
    val status: String
)
