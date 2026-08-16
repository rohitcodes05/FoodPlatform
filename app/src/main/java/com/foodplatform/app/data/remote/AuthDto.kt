package com.foodplatform.app.data.remote

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val phone: String,
    val createdAt: String,
    val updatedAt: String
)
