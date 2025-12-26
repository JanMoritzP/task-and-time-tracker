package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_data")
data class AppData(
    @PrimaryKey
    val id: String,
    val name: String,
    var packageName: String,
    var hardCap: Int,
    val costPerMinute: Float = 0f,
    val timeSpent: Long = 0L
)
