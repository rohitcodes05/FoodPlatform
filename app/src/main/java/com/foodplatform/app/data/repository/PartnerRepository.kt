package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.CreatePartnerProductRequest
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.PartnerApi
import com.foodplatform.app.data.remote.PartnerProductDto
import com.foodplatform.app.data.remote.UpdatePartnerOrderStatusRequest
import com.foodplatform.app.data.remote.UpdatePartnerProductRequest
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartnerRepository(private val partnerApi: PartnerApi) {

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

    // ── Orders ────────────────────────────────────────────────────────────────

    suspend fun getPartnerOrders(): Result<List<OrderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.getPartnerOrders()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to fetch orders")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePartnerOrderStatus(orderId: String, status: String): Result<OrderDto> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.updatePartnerOrderStatus(orderId, UpdatePartnerOrderStatusRequest(status))
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

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getPartnerProducts(): Result<List<PartnerProductDto>> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.getPartnerProducts()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to fetch products")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPartnerProduct(request: CreatePartnerProductRequest): Result<PartnerProductDto> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.createPartnerProduct(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body) else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to create product")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePartnerProduct(productId: String, request: UpdatePartnerProductRequest): Result<PartnerProductDto> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.updatePartnerProduct(productId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body) else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to update product")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePartnerProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = partnerApi.deletePartnerProduct(productId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = response.errorBody()?.string()
                val msg = if (response.code() == 409) {
                    "Product has existing orders and cannot be deleted"
                } else {
                    parseErrorMessage(errBody, "Failed to delete product")
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
