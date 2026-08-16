package com.foodplatform.app.data.remote

import android.util.Log
import com.foodplatform.app.data.local.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class AuthInterceptor(
    private val tokenStorage: SecureTokenStorage,
    private val onUnauthorized: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenStorage.getToken()

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Only handle 401 Unauthorized for session invalidation.
        // DO NOT handle 403 Forbidden here as that is an authorization issue, not authentication.
        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            tokenStorage.clearToken()
            onUnauthorized()
        }

        return response
    }
}
