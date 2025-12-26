package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_definition")
data class RewardDefinition(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val coinCost: Int = 0,
    val archived: Boolean = false,
)
