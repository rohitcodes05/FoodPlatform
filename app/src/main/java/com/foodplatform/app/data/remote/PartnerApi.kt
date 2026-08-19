package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PartnerApi {

    // ── Orders ────────────────────────────────────────────────────────────────

    @GET("partners/orders")
    suspend fun getPartnerOrders(): Response<List<OrderDto>>

    @PATCH("partners/orders/{id}/status")
    suspend fun updatePartnerOrderStatus(
        @Path("id") id: String,
        @Body request: UpdatePartnerOrderStatusRequest
    ): Response<OrderDto>

    // ── Products ──────────────────────────────────────────────────────────────

    @GET("partners/products")
    suspend fun getPartnerProducts(): Response<List<PartnerProductDto>>

    @POST("partners/products")
    suspend fun createPartnerProduct(
        @Body request: CreatePartnerProductRequest
    ): Response<PartnerProductDto>

    @PATCH("partners/products/{id}")
    suspend fun updatePartnerProduct(
        @Path("id") id: String,
        @Body request: UpdatePartnerProductRequest
    ): Response<PartnerProductDto>

    @DELETE("partners/products/{id}")
    suspend fun deletePartnerProduct(
        @Path("id") id: String
    ): Response<Unit>
}
