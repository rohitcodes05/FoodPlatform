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
    val cutOptionId: String?,
    val weightOptionId: String?,
    val quantity: Double,
    val product: ProductDto,
    val cutOption: CutOptionDto?,
    val weightOption: WeightOptionDto?
)

data class AddCartItemRequest(
    val productId: String,
    val quantity: Int,
    val cutOptionId: String? = null,
    val weightOptionId: String? = null
)

data class UpdateCartItemRequest(
    val quantity: Int
)
