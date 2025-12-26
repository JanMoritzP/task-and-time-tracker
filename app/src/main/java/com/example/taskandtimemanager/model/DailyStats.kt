package com.example.taskandtimemanager.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyStats(
    val date: String,
    val coinsEarned: Int = 0,
    val tasksCompleted: Int = 0,
    val infiniteTasksCount: Int = 0,
    val timeSpentOnApps: Int = 0,
    val usage: List<UsageRecord> = emptyList(),
    val rewardsRedeemed: Int = 0,
)
