package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_aggregate")
data class AppUsageAggregate(
    @PrimaryKey
    val id: String,
    val appId: String,
    val date: String,
    val usedMinutesAutomatic: Long,
)
