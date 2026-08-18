package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.CreateOrderRequest
import com.foodplatform.app.data.remote.OrderApi
import com.foodplatform.app.data.remote.OrderDto
import com.google.gson.JsonParser

open class OrderRepository(private val api: OrderApi) {
    open suspend fun createOrder(addressId: String): Result<OrderDto> {
        return try {
            val response = api.createOrder(CreateOrderRequest(addressId))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Order placed but empty response"))
                }
            } else {
                val errorMsg = try {
                    val errorString = response.errorBody()?.string()
                    if (errorString != null) {
                        val jsonElement = JsonParser().parse(errorString)
                        if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("message")) {
                            jsonElement.asJsonObject.get("message").asString
                        } else {
                            "Failed to place order"
                        }
                    } else {
                        "Failed to place order"
                    }
                } catch (e: Exception) {
                    "Failed to place order"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
