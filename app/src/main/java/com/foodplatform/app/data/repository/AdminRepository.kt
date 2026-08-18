package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.AdminApi
import com.foodplatform.app.data.remote.DeliveryDto
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.UpdateDeliveryStatusRequest
import com.foodplatform.app.data.remote.UpdateOrderStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonParser
import retrofit2.Response

class AdminRepository(private val adminApi: AdminApi) {
    private fun parseErrorMessage(errorBody: String?, defaultMsg: String): String {
        if (errorBody.isNullOrEmpty()) return defaultMsg
        return try {
            val jsonElement = JsonParser().parse(errorBody)
            if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("message")) {
                val msgNode = jsonElement.asJsonObject.get("message")
                if (msgNode.isJsonArray) {
                    msgNode.asJsonArray.joinToString(", ") { it.asString }
                } else {
                    msgNode.asString
                }
            } else {
                defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }
    suspend fun getOrders(): Result<List<OrderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = adminApi.getAdminOrders()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to fetch orders")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<OrderDto> = withContext(Dispatchers.IO) {
        try {
            val response = adminApi.updateOrderStatus(orderId, UpdateOrderStatusRequest(status))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body) else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to update order status")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryStatus(deliveryId: String, status: String, trackingCode: String?): Result<DeliveryDto> = withContext(Dispatchers.IO) {
        try {
            val response = adminApi.updateDeliveryStatus(deliveryId, UpdateDeliveryStatusRequest(status, trackingCode))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body) else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to update delivery status")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
