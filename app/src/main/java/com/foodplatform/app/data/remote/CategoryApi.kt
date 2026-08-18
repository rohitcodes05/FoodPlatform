package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface CategoryApi {
    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>
}
