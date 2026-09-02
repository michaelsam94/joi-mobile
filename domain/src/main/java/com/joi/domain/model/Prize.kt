package com.joi.domain.model

data class Prize(
    val id: String,
    val name: String,
    val description: String?,
    val pointsCost: Int,
    val imageUrl: String?,
    val active: Boolean,
    /** Remaining stock. Null means unlimited. 0 means out of stock — redemption is blocked. */
    val quantity: Int? = null,
)

data class PrizeRedemption(
    val id: String,
    val prizeId: String,
    val userId: String,
    val pointsSpent: Int,
    val createdAt: String,
)
