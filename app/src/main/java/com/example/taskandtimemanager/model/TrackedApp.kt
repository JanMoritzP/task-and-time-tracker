package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_app")
data class TrackedApp(
    @PrimaryKey
    val id: String,
    val name: String,
    val packageName: String,
    val costPerMinute: Float = 0f,
    val purchasedMinutesTotal: Long = 0L,
    val isBlocked: Boolean = false,
)
