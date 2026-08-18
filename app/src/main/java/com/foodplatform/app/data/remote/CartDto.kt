package com.foodplatform.app.data.remote

data class CartDto(
    val id: String,
    val userId: String,
    val items: List<CartItemDto>
)

data class CartItemDto(
    val id: String,
    val cartId: String,
    val productId: String,
    val quantity: Double,
    val product: ProductDto
)

data class AddCartItemRequest(
    val productId: String,
    val quantity: Int
)

data class UpdateCartItemRequest(
    val quantity: Int
)
