package com.foodplatform.app.data.remote

data class UpdateOrderStatusRequest(
    val status: String
)

data class UpdateDeliveryStatusRequest(
    val status: String,
    val trackingCode: String? = null
)
