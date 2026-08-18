package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.AddressApi
import com.foodplatform.app.data.remote.AddressDto
import com.foodplatform.app.data.remote.CreateAddressRequest
import com.foodplatform.app.data.remote.UpdateAddressRequest

open class AddressRepository(private val api: AddressApi) {
    open suspend fun getAddresses(): Result<List<AddressDto>> {
        return try {
            val response = api.getAddresses()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load addresses"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun createAddress(street: String, city: String, state: String, postalCode: String, country: String): Result<AddressDto> {
        return try {
            val response = api.createAddress(CreateAddressRequest(street, city, state, postalCode, country))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to add address"))
                }
            } else {
                Result.failure(Exception("Failed to add address"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun updateAddress(id: String, street: String, city: String, state: String, postalCode: String, country: String): Result<AddressDto> {
        return try {
            val response = api.updateAddress(id, UpdateAddressRequest(street, city, state, postalCode, country))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to update address"))
                }
            } else {
                Result.failure(Exception("Failed to update address"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun deleteAddress(id: String): Result<AddressDto> {
        return try {
            val response = api.deleteAddress(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to delete address"))
                }
            } else {
                Result.failure(Exception("Failed to delete address"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
