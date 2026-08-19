package com.foodplatform.app.data.remote

// ── Product (partner-owned) ────────────────────────────────────────────────────
// Extends the base ProductDto shape; partnerId is optional and read-only (display only).
// NOTE: partnerId is NEVER sent in any request body — backend derives it from JWT.
data class PartnerProductDto(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val price: Double,
    val isAvailable: Boolean,
    val partnerId: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val cutOptions: List<CutOptionDto> = emptyList(),
    val weightOptions: List<WeightOptionDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// ── Request DTOs ──────────────────────────────────────────────────────────────
// IMPORTANT: None of these contain partnerId.
// Backend derives partner ownership from the Bearer JWT token.

data class CreatePartnerProductRequest(
    val name: String,
    val description: String? = null,
    val type: String,
    val price: Double,
    val isAvailable: Boolean = true
    // categoryIds intentionally omitted (Phase 11D MVP — no category picker)
)

data class UpdatePartnerProductRequest(
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val price: Double? = null,
    val isAvailable: Boolean? = null
    // categoryIds intentionally omitted (Phase 11D MVP)
)

data class UpdatePartnerOrderStatusRequest(
    val status: String
)

data class CreateCutOptionRequest(
    val name: String,
    val isAvailable: Boolean = true
)

data class UpdateCutOptionRequest(
    val name: String? = null,
    val isAvailable: Boolean? = null
)

data class CreateWeightOptionRequest(
    val weightLabel: String,
    val priceOverride: Double,
    val isAvailable: Boolean = true
)

data class UpdateWeightOptionRequest(
    val weightLabel: String? = null,
    val priceOverride: Double? = null,
    val isAvailable: Boolean? = null
)
