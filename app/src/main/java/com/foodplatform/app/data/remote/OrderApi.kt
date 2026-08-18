package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OrderApi {
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderDto>
}
