package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.CategoryApi
import com.foodplatform.app.data.remote.CategoryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

open class CategoryRepository(private val api: CategoryApi) {

    open suspend fun getCategories(): Result<List<CategoryDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
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
