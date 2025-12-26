package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_definition")
data class TaskDefinition(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String = "",
    val mandatory: Boolean = false,
    val rewardCoins: Int = 0,
    val recurrenceType: String = "DAILY",
    val maxExecutionsPerDay: Int? = null,
    val archived: Boolean = false,
)
