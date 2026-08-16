package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.PaginatedResponse
import com.foodplatform.app.data.remote.ProductApi
import com.foodplatform.app.data.remote.ProductDto

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

class ProductRepository(
    private val productApi: ProductApi
) {
    suspend fun getProducts(
        page: Int = 1,
        limit: Int = 20,
        categoryId: String? = null,
        type: String? = null,
        search: String? = null
    ): Resource<PaginatedResponse<ProductDto>> {
        return try {
            val response = productApi.getProducts(
                page = page,
                limit = limit,
                categoryId = categoryId,
                type = type,
                search = search
            )
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred fetching products")
        }
    }

    suspend fun getProductById(id: String): Resource<ProductDto> {
        return try {
            val response = productApi.getProductById(id)
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred fetching product details")
        }
    }
}
