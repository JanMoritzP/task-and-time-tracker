package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_purchase")
data class AppUsagePurchase(
    @PrimaryKey
    val id: String,
    val appId: String,
    val minutesPurchased: Long,
    val coinsSpent: Int,
    val purchaseDateTime: String,
)
