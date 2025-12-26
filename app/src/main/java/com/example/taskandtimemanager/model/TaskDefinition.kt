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
    /** Base reward for the first completion in a day. */
    val rewardCoins: Int = 0,
    /** Optional alternate reward for recurring executions (e.g. infinite tasks). */
    val recurringRewardCoins: Int? = null,
    val recurrenceType: String = "DAILY",
    val maxExecutionsPerDay: Int? = null,
    val archived: Boolean = false,
)
