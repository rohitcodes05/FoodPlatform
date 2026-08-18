package com.foodplatform.app.data.remote

import com.foodplatform.app.data.local.SecureTokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // Development local URL. For emulator, use 10.0.2.2.
    // Base URL is NOT a secret.
    private const val BASE_URL = "http://10.0.2.2:3000/"

    fun provideRetrofit(
        tokenStorage: SecureTokenStorage,
        onUnauthorized: () -> Unit
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // Redacted for security, enable only if absolutely needed for dev, but strip headers/bodies.
        }

        val authInterceptor = AuthInterceptor(tokenStorage, onUnauthorized)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    fun provideProductApi(retrofit: Retrofit): ProductApi {
        return retrofit.create(ProductApi::class.java)
    }

    fun provideCartApi(retrofit: Retrofit): CartApi {
        return retrofit.create(CartApi::class.java)
    }

    fun provideAddressApi(retrofit: Retrofit): AddressApi {
        return retrofit.create(AddressApi::class.java)
    }

    fun provideOrderApi(retrofit: Retrofit): OrderApi {
        return retrofit.create(OrderApi::class.java)
    }

    fun provideCategoryApi(retrofit: Retrofit): CategoryApi {
        return retrofit.create(CategoryApi::class.java)
    }

    fun provideReviewApi(retrofit: Retrofit): ReviewApi {
        return retrofit.create(ReviewApi::class.java)
    }
}
