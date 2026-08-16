package com.foodplatform.app.data.repository

import com.foodplatform.app.data.local.SecureTokenStorage
import com.foodplatform.app.data.remote.AuthApi
import com.foodplatform.app.data.remote.LoginRequest
import com.foodplatform.app.data.remote.RegisterRequest
import com.foodplatform.app.data.remote.UserResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(
    private val api: AuthApi,
    private val tokenStorage: SecureTokenStorage
) {

    suspend fun registerAndLogin(request: RegisterRequest): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Register
            val registerResponse = api.register(request)
            if (!registerResponse.isSuccessful) {
                return@withContext AuthResult.Error(parseErrorMessage(registerResponse.errorBody()?.string()))
            }

            // 2. Auto-Login
            val loginRequest = LoginRequest(email = request.email, password = request.password)
            val loginResponse = api.login(loginRequest)
            
            if (loginResponse.isSuccessful) {
                val token = loginResponse.body()?.accessToken
                if (!token.isNullOrEmpty()) {
                    tokenStorage.saveToken(token)
                    return@withContext AuthResult.Success(Unit)
                } else {
                    return@withContext AuthResult.Error("Failed to retrieve access token.")
                }
            } else {
                return@withContext AuthResult.Error("Registration succeeded, but auto-login failed: ${parseErrorMessage(loginResponse.errorBody()?.string())}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun login(request: LoginRequest): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(request)
            if (response.isSuccessful) {
                val token = response.body()?.accessToken
                if (!token.isNullOrEmpty()) {
                    tokenStorage.saveToken(token)
                    AuthResult.Success(Unit)
                } else {
                    AuthResult.Error("Invalid token received.")
                }
            } else {
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun getCurrentUser(): AuthResult<UserResponse> = withContext(Dispatchers.IO) {
        if (tokenStorage.getToken().isNullOrEmpty()) {
            return@withContext AuthResult.Error("No local token found.")
        }
        
        try {
            val response = api.getMe()
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Empty user data.")
                }
            } else {
                // Interceptor handles 401 token clear. Just return error here.
                AuthResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    fun logout() {
        // Backend stateless JWT: client-side clear is sufficient.
        tokenStorage.clearToken()
    }

    fun hasToken(): Boolean {
        return !tokenStorage.getToken().isNullOrEmpty()
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) return "Unknown error occurred"
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = Gson().fromJson(errorBody, type)
            if (map.containsKey("message")) {
                val msg = map["message"]
                if (msg is List<*>) {
                    msg.joinToString(", ")
                } else {
                    msg.toString()
                }
            } else {
                errorBody
            }
        } catch (e: Exception) {
            "An error occurred"
        }
    }
}
