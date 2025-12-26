package com.example.taskandtimemanager.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteApp(
    val appId: String,
    val name: String,
    val purchaseCount: Int,
)
