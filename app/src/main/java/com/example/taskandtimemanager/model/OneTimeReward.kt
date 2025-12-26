package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "one_time_reward")
data class OneTimeReward(
    @PrimaryKey
    val id: String,
    val name: String = "",
    val description: String = "",
    val coins: Int = 0,
    val claimedDate: String = "",
    val claimedTime: String = ""
)
