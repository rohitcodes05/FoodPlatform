package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.CreateOrderRequest
import com.foodplatform.app.data.remote.OrderApi
import com.foodplatform.app.data.remote.OrderDto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OrderRepositoryTest {

    private val mockApi = object : OrderApi {
        var shouldSucceed = true
        var errorMessage = "{\"message\":\"Out of stock\"}"

        override suspend fun createOrder(request: CreateOrderRequest): Response<OrderDto> {
            return if (shouldSucceed) {
                Response.success(OrderDto("1", "user1", "100.00", "PENDING", "2026-08-18T10:00:00Z"))
            } else {
                Response.error(400, errorMessage.toResponseBody())
            }
        }

        override suspend fun getOrders(): Response<List<OrderDto>> {
            return if (shouldSucceed) {
                Response.success(listOf(OrderDto("1", "user1", "100.00", "PENDING", "2026-08-18T10:00:00Z")))
            } else {
                Response.error(400, errorMessage.toResponseBody())
            }
        }

        override suspend fun getOrderById(id: String): Response<OrderDto> {
            return if (shouldSucceed) {
                Response.success(OrderDto("1", "user1", "100.00", "PENDING", "2026-08-18T10:00:00Z"))
            } else {
                Response.error(400, errorMessage.toResponseBody())
            }
        }
    }

    private val repository = OrderRepository(mockApi)

    @Test
    fun `createOrder success returns order dto`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.createOrder("address1")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull()?.id)
    }

    @Test
    fun `createOrder failure parses backend error message`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.createOrder("address1")
        assertTrue(result.isFailure)
        assertEquals("Out of stock", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getOrders success returns order list`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.getOrders()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getOrders failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.getOrders()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getOrderById success returns order dto`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.getOrderById("1")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull()?.id)
    }

    @Test
    fun `getOrderById failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.getOrderById("1")
        assertTrue(result.isFailure)
    }
}
