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

    // ── Variations (Cuts) ───────────────────────────────────────────────────

    @POST("partners/products/{id}/cuts")
    suspend fun createCutOption(
        @Path("id") productId: String,
        @Body request: CreateCutOptionRequest
    ): Response<CutOptionDto>

    @PATCH("partners/products/{id}/cuts/{cutId}")
    suspend fun updateCutOption(
        @Path("id") productId: String,
        @Path("cutId") cutId: String,
        @Body request: UpdateCutOptionRequest
    ): Response<CutOptionDto>

    @DELETE("partners/products/{id}/cuts/{cutId}")
    suspend fun deleteCutOption(
        @Path("id") productId: String,
        @Path("cutId") cutId: String
    ): Response<Unit>

    // ── Variations (Weights) ────────────────────────────────────────────────

    @POST("partners/products/{id}/weights")
    suspend fun createWeightOption(
        @Path("id") productId: String,
        @Body request: CreateWeightOptionRequest
    ): Response<WeightOptionDto>

    @PATCH("partners/products/{id}/weights/{weightId}")
    suspend fun updateWeightOption(
        @Path("id") productId: String,
        @Path("weightId") weightId: String,
        @Body request: UpdateWeightOptionRequest
    ): Response<WeightOptionDto>

    @DELETE("partners/products/{id}/weights/{weightId}")
    suspend fun deleteWeightOption(
        @Path("id") productId: String,
        @Path("weightId") weightId: String
    ): Response<Unit>
}
