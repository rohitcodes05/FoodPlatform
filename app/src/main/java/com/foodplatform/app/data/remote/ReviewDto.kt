package com.foodplatform.app.data.remote

data class UserSummaryDto(
    val name: String
)

data class ReviewDto(
    val id: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val user: UserSummaryDto? = null
)

data class CreateReviewRequest(
    val orderItemId: String,
    val rating: Int,
    val comment: String?
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String?
)
