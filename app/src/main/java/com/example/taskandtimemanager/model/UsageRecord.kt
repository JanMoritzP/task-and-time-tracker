package com.example.taskandtimemanager.model

import kotlinx.serialization.Serializable

@Serializable
data class UsageRecord(
    val appId: String,
    val appName: String,
    val date: String,
    val minutesUsed: Int,
    val timeRange: String,
)
