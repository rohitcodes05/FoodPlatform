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

// ── Variations (Cuts) ───────────────────────────────────────────────────

suspend fun createCutOption(productId: String, name: String, isAvailable: Boolean = true) = withContext(Dispatchers.IO) {
    try {
        val request = com.foodplatform.app.data.remote.CreateCutOptionRequest(name, isAvailable)
        val response = partnerApi.createCutOption(productId, request)
        if (response.isSuccessful) Result.success(response.body()!!) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to create cut option")))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun updateCutOption(productId: String, cutId: String, name: String?, isAvailable: Boolean?) = withContext(Dispatchers.IO) {
    try {
        val request = com.foodplatform.app.data.remote.UpdateCutOptionRequest(name, isAvailable)
        val response = partnerApi.updateCutOption(productId, cutId, request)
        if (response.isSuccessful) Result.success(response.body()!!) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to update cut option")))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun deleteCutOption(productId: String, cutId: String) = withContext(Dispatchers.IO) {
    try {
        val response = partnerApi.deleteCutOption(productId, cutId)
        if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to delete cut option")))
    } catch (e: Exception) { Result.failure(e) }
}

// ── Variations (Weights) ────────────────────────────────────────────────

suspend fun createWeightOption(productId: String, weightLabel: String, priceOverride: Double, isAvailable: Boolean = true) = withContext(Dispatchers.IO) {
    try {
        val request = com.foodplatform.app.data.remote.CreateWeightOptionRequest(weightLabel, priceOverride, isAvailable)
        val response = partnerApi.createWeightOption(productId, request)
        if (response.isSuccessful) Result.success(response.body()!!) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to create weight option")))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun updateWeightOption(productId: String, weightId: String, weightLabel: String?, priceOverride: Double?, isAvailable: Boolean?) = withContext(Dispatchers.IO) {
    try {
        val request = com.foodplatform.app.data.remote.UpdateWeightOptionRequest(weightLabel, priceOverride, isAvailable)
        val response = partnerApi.updateWeightOption(productId, weightId, request)
        if (response.isSuccessful) Result.success(response.body()!!) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to update weight option")))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun deleteWeightOption(productId: String, weightId: String) = withContext(Dispatchers.IO) {
    try {
        val response = partnerApi.deleteWeightOption(productId, weightId)
        if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseErrorMessage(response.errorBody()?.string(), "Failed to delete weight option")))
    } catch (e: Exception) { Result.failure(e) }
}
}
