package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_execution")
data class TaskExecution(
    @PrimaryKey
    val id: String,
    val taskDefinitionId: String,
    val date: String,
    val time: String? = null,
    val status: String = "DONE",
    val coinsAwarded: Int = 0,
)
