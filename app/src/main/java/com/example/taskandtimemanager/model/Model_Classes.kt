// ============ DATA MODELS ============
// File: model/Task.kt
package com.example.taskandtimemanager.model

import kotlinx.serialization.Serializable

@Serializable
data class UsageRecord(
    val appId: String,
    val appName: String,
    val date: String,
    val minutesUsed: Int,
    val timeRange: String // "09:30-10:45"
)

@Serializable
data class DailyStats(
    val date: String,
    val coinsEarned: Int = 0,
    val tasksCompleted: Int = 0,
    val infiniteTasksCount: Int = 0,
    val timeSpentOnApps: Int = 0, // total minutes
    val usage: List<UsageRecord> = emptyList(),
    val oneTimeRewardsClaimed: Int = 0
)

// File: model/FavoriteApp.kt
@Serializable
data class FavoriteApp(
    val appId: String,
    val name: String,
    val purchaseCount: Int // auto-detected
)
