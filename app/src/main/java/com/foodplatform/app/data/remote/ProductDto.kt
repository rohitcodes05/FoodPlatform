package com.foodplatform.app.data.remote

enum class ProductType {
    COOKED_FOOD,
    RAW_MEAT
}

data class CategoryDto(
    val id: String,
    val name: String
)

data class CutOptionDto(
    val id: String,
    val productId: String,
    val name: String,
    val isAvailable: Boolean
)

data class WeightOptionDto(
    val id: String,
    val productId: String,
    val weightLabel: String,
    val priceOverride: Double,
    val isAvailable: Boolean
)

data class ProductDto(
    val id: String,
    val name: String,
    val description: String?,
    val type: ProductType,
    val price: Double,
    val isAvailable: Boolean,
    val categories: List<CategoryDto> = emptyList(),
    val cutOptions: List<CutOptionDto> = emptyList(),
    val weightOptions: List<WeightOptionDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class PaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val meta: PaginationMeta
)
