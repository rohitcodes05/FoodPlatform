package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AddressApi {
    @GET("addresses")
    suspend fun getAddresses(): Response<List<AddressDto>>

    @POST("addresses")
    suspend fun createAddress(@Body request: CreateAddressRequest): Response<AddressDto>

    @PATCH("addresses/{id}")
    suspend fun updateAddress(@retrofit2.http.Path("id") id: String, @Body request: UpdateAddressRequest): Response<AddressDto>

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(@retrofit2.http.Path("id") id: String): Response<AddressDto>
}
