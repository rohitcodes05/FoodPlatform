package com.foodplatform.app.data.remote

data class CreateOrderRequest(
    val addressId: String
)

data class DeliveryDto(
    val id: String,
    val status: String,
    val trackingCode: String? = null
)

data class PaymentDto(
    val status: String,
    val amount: String
)

data class OrderDto(
    val id: String,
    val userId: String,
    val totalAmount: String,
    val status: String,
    val createdAt: String,
    val items: List<OrderItemDto>? = null,
    val address: OrderAddressSnapshotDto? = null,
    val delivery: DeliveryDto? = null,
    val payment: PaymentDto? = null
)

data class OrderItemDto(
    val id: String,
    val quantity: String,
    val purchasePrice: String,
    val selectedCut: String?,
    val selectedWeight: String?,
    val product: ProductDto,
    val review: ReviewDto? = null
)

data class OrderAddressSnapshotDto(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)
