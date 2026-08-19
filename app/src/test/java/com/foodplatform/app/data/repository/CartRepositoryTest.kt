package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.AddCartItemRequest
import com.foodplatform.app.data.remote.CartApi
import com.foodplatform.app.data.remote.CartDto
import com.foodplatform.app.data.remote.CartItemDto
import com.foodplatform.app.data.remote.UpdateCartItemRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartRepositoryTest {

    class FakeCartApi : CartApi {
        var shouldThrow = false
        val cartDto = CartDto("1", "user1", emptyList())

        override suspend fun getCart(): CartDto {
            if (shouldThrow) throw RuntimeException("Network error")
            return cartDto
        }

        override suspend fun addItem(request: AddCartItemRequest): CartItemDto {
            if (shouldThrow) throw RuntimeException("Error")
            return CartItemDto("item1", "cart1", request.productId, null, null, request.quantity.toDouble(), mockProduct(), null, null)
        }

        override suspend fun updateItemQuantity(productId: String, request: UpdateCartItemRequest): CartItemDto {
            if (shouldThrow) throw RuntimeException("Error")
            return CartItemDto("item1", "cart1", productId, null, null, request.quantity.toDouble(), mockProduct(), null, null)
        }

        override suspend fun removeItem(productId: String) {
            if (shouldThrow) throw RuntimeException("Error")
        }

        override suspend fun clearCart() {
            if (shouldThrow) throw RuntimeException("Error")
        }

        private fun mockProduct() = com.foodplatform.app.data.remote.ProductDto("p1", "Burger", null, com.foodplatform.app.data.remote.ProductType.COOKED_FOOD, 10.0, true)
    }

    private lateinit var cartApi: FakeCartApi
    private lateinit var repository: CartRepository

    @Before
    fun setup() {
        cartApi = FakeCartApi()
        repository = CartRepository(cartApi)
    }

    @Test
    fun getCart_success_returnsSuccess() = runBlocking {
        val result = repository.getCart()

        assertTrue(result is Resource.Success)
        assertEquals(cartApi.cartDto, (result as Resource.Success).data)
    }

    @Test
    fun getCart_error_returnsError() = runBlocking {
        cartApi.shouldThrow = true
        val result = repository.getCart()

        assertTrue(result is Resource.Error)
        assertEquals("Network error", (result as Resource.Error).message)
    }

    @Test
    fun addItem_success_returnsSuccess() = runBlocking {
        val result = repository.addItem("prod1", 2)
        assertTrue(result is Resource.Success)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cartUpdates_emittedOnMutations() = runTest {
        var emitCount = 0
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.cartUpdates.collect { _: Unit ->
                emitCount++
            }
        }

        repository.addItem("prod1", 1)
        repository.updateItemQuantity("prod1", 2)
        repository.removeItem("prod1")
        repository.clearCart()

        assertEquals(4, emitCount)
        job.cancel()
    }
}
