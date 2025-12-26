package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_redemption")
data class RewardRedemption(
    @PrimaryKey
    val id: String,
    val rewardDefinitionId: String,
    val redemptionDateTime: String,
    val coinsSpent: Int,
)
