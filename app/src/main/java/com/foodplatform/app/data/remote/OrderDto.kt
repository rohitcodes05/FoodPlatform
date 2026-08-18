package com.foodplatform.app.data.remote

data class CreateOrderRequest(
    val addressId: String
)

data class OrderDto(
    val id: String,
    val userId: String,
    val totalAmount: String,
    val status: String,
    val createdAt: String,
    val items: List<OrderItemDto>? = null,
    val address: OrderAddressSnapshotDto? = null
)

data class OrderItemDto(
    val id: String,
    val quantity: String,
    val purchasePrice: String,
    val product: ProductDto
)

data class OrderAddressSnapshotDto(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)
