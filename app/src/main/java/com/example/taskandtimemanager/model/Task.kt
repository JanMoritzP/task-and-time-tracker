package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task")
data class Task(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String = "",
    val completed: Boolean = false,
    val mandatory: Boolean = false,
    val reward: Int = 0,
    val infinite: Boolean = false,
    val infiniteName: String = "",
    val infiniteReward: Int = 0,
    val infiniteCount: Int = 0
)
