package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.PaginatedResponse
import com.foodplatform.app.data.remote.PaginationMeta
import com.foodplatform.app.data.remote.ProductApi
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.remote.ProductType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRepositoryTest {

    class FakeProductApi(
        var getProductsResponse: PaginatedResponse<ProductDto>? = null,
        var getProductByIdResponse: ProductDto? = null,
        var shouldThrow: Boolean = false
    ) : ProductApi {
        override suspend fun getProducts(page: Int?, limit: Int?, categoryId: String?, type: String?, search: String?): PaginatedResponse<ProductDto> {
            if (shouldThrow) throw RuntimeException("Network error")
            return getProductsResponse!!
        }

        override suspend fun getProductById(id: String): ProductDto {
            if (shouldThrow) throw RuntimeException("Network error")
            return getProductByIdResponse!!
        }
    }

    @Test
    fun `getProducts returns success when api call succeeds`() = runBlocking {
        val dummyProducts = listOf(
            ProductDto("1", "Burger", "Tasty", ProductType.COOKED_FOOD, 5.99, true)
        )
        val response = PaginatedResponse(dummyProducts, PaginationMeta(1, 1, 20, 1))
        
        val mockApi = FakeProductApi(getProductsResponse = response)
        val repository = ProductRepository(mockApi)

        val result = repository.getProducts(1, 20)

        assertTrue(result is Resource.Success)
        assertEquals(1, (result as Resource.Success).data.items.size)
        assertEquals("Burger", result.data.items[0].name)
    }

    @Test
    fun `getProducts returns error when api call fails`() = runBlocking {
        val mockApi = FakeProductApi(shouldThrow = true)
        val repository = ProductRepository(mockApi)

        val result = repository.getProducts(1, 20)

        assertTrue(result is Resource.Error)
        assertEquals("Network error", (result as Resource.Error).message)
    }
}
