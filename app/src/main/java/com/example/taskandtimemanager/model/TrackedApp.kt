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
    /**
     * When true, this app is exempt from blocking checks during the current
     * night session. The flag is automatically cleared after 06:00 local time
     * by the blocker service when it next evaluates the app.
     */
    val nightOverrideEnabled: Boolean = false,
)
