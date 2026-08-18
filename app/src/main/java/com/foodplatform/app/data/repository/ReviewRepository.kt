package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.ReviewApi
import com.foodplatform.app.data.remote.ReviewDto
import com.foodplatform.app.data.remote.CreateReviewRequest
import com.foodplatform.app.data.remote.UpdateReviewRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

open class ReviewRepository(private val api: ReviewApi) {

    open suspend fun getReviewsByProduct(productId: String): Result<List<ReviewDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReviewsByProduct(productId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message ?: "Unknown error"}"))
        }
    }

    open suspend fun createReview(orderItemId: String, rating: Int, comment: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.createReview(CreateReviewRequest(orderItemId, rating, comment))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message ?: "Unknown error"}"))
        }
    }

    open suspend fun updateReview(id: String, rating: Int, comment: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateReview(id, UpdateReviewRequest(rating, comment))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message ?: "Unknown error"}"))
        }
    }

    open suspend fun deleteReview(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteReview(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message ?: "Unknown error"}"))
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) return "Unknown error"
        return try {
            val jsonObject = JSONObject(errorBody)
            if (jsonObject.has("message")) {
                val message = jsonObject.get("message")
                if (message is org.json.JSONArray) {
                    message.join(", ").replace("\"", "")
                } else {
                    message.toString()
                }
            } else {
                "Unknown error"
            }
        } catch (e: Exception) {
            errorBody
        }
    }
}
