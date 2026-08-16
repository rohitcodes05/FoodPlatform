package com.foodplatform.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("type") type: String? = null,
        @Query("search") search: String? = null
    ): PaginatedResponse<ProductDto>

    @GET("products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): ProductDto
}
