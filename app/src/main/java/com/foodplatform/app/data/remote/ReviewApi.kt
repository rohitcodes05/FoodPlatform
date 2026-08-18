package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ReviewApi {
    @GET("reviews/product/{productId}")
    suspend fun getReviewsByProduct(@Path("productId") productId: String): Response<List<ReviewDto>>

    @POST("reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Response<Unit>

    @PATCH("reviews/{id}")
    suspend fun updateReview(@Path("id") id: String, @Body request: UpdateReviewRequest): Response<Unit>

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): Response<Unit>
}
