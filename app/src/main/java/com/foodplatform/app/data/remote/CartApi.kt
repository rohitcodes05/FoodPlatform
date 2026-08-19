package com.foodplatform.app.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CartApi {
    @GET("cart")
    suspend fun getCart(): CartDto

    @POST("cart/items")
    suspend fun addItem(@Body request: AddCartItemRequest): CartItemDto

    @PATCH("cart/items/{itemId}")
    suspend fun updateItemQuantity(
        @Path("itemId") itemId: String,
        @Body request: UpdateCartItemRequest
    ): CartItemDto

    @DELETE("cart/items/{itemId}")
    suspend fun removeItem(@Path("itemId") itemId: String)

    @DELETE("cart")
    suspend fun clearCart()
}
