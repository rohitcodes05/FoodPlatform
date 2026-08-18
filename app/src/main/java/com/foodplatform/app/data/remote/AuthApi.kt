package com.foodplatform.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserResponse>

    @retrofit2.http.PATCH("auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserResponse>
}
