package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.AddressApi
import com.foodplatform.app.data.remote.AddressDto
import com.foodplatform.app.data.remote.CreateAddressRequest
import com.foodplatform.app.data.remote.UpdateAddressRequest
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AddressRepositoryTest {

    private val mockApi = object : AddressApi {
        var shouldSucceed = true
        var addresses = listOf(AddressDto("1", "Street", "City", "State", "123", "Country"))

        override suspend fun getAddresses(): Response<List<AddressDto>> {
            return if (shouldSucceed) {
                Response.success(addresses)
            } else {
                Response.error(500, "Error".toResponseBody())
            }
        }

        override suspend fun createAddress(request: CreateAddressRequest): Response<AddressDto> {
            return if (shouldSucceed) {
                Response.success(AddressDto("2", request.street, request.city, request.state, request.postalCode, request.country))
            } else {
                Response.error(400, "Error".toResponseBody())
            }
        }

        override suspend fun updateAddress(id: String, request: UpdateAddressRequest): Response<AddressDto> {
            return if (shouldSucceed) {
                Response.success(AddressDto(id, request.street ?: "", request.city ?: "", request.state ?: "", request.postalCode ?: "", request.country ?: ""))
            } else {
                Response.error(400, "Error".toResponseBody())
            }
        }

        override suspend fun deleteAddress(id: String): Response<AddressDto> {
            return if (shouldSucceed) {
                Response.success(AddressDto(id, "Street", "City", "State", "123", "Country"))
            } else {
                Response.error(400, "Error".toResponseBody())
            }
        }
    }

    private val repository = AddressRepository(mockApi)

    @Test
    fun `getAddresses success returns list of addresses`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.getAddresses()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getAddresses failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.getAddresses()
        assertTrue(result.isFailure)
    }

    @Test
    fun `createAddress success returns created address`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.createAddress("Street 2", "City", "State", "123", "Country")
        assertTrue(result.isSuccess)
        assertEquals("2", result.getOrNull()?.id)
        assertEquals("Street 2", result.getOrNull()?.street)
    }

    @Test
    fun `createAddress failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.createAddress("Street 2", "City", "State", "123", "Country")
        assertTrue(result.isFailure)
    }

    @Test
    fun `updateAddress success returns updated address`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.updateAddress("1", "Street 3", "City", "State", "123", "Country")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull()?.id)
        assertEquals("Street 3", result.getOrNull()?.street)
    }

    @Test
    fun `updateAddress failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.updateAddress("1", "Street 3", "City", "State", "123", "Country")
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAddress success returns deleted address`() = runBlocking {
        mockApi.shouldSucceed = true
        val result = repository.deleteAddress("1")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull()?.id)
    }

    @Test
    fun `deleteAddress failure returns error`() = runBlocking {
        mockApi.shouldSucceed = false
        val result = repository.deleteAddress("1")
        assertTrue(result.isFailure)
    }
}
