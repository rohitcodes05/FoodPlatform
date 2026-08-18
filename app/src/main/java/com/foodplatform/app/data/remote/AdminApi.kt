package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface AdminApi {
    @GET("admin/orders")
    suspend fun getAdminOrders(): Response<List<OrderDto>>

    @PATCH("admin/orders/{id}/status")
    suspend fun updateOrderStatus(@Path("id") id: String, @Body request: UpdateOrderStatusRequest): Response<OrderDto>

    @PATCH("admin/deliveries/{id}/status")
    suspend fun updateDeliveryStatus(@Path("id") id: String, @Body request: UpdateDeliveryStatusRequest): Response<DeliveryDto>
}
