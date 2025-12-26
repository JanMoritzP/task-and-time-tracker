package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.RewardDefinition
import com.example.taskandtimemanager.model.RewardRedemption
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import com.example.taskandtimemanager.model.TrackedApp
import kotlinx.serialization.Serializable

/**
 * Serializable wrapper that contains the complete logical state of the app
 * that must be preserved across export / import operations.
 */
@Serializable
 data class ExportImportState(
     val taskDefinitions: List<@kotlinx.serialization.Contextual TaskDefinition> = emptyList(),
     val taskExecutions: List<@kotlinx.serialization.Contextual TaskExecution> = emptyList(),
     val rewardDefinitions: List<@kotlinx.serialization.Contextual RewardDefinition> = emptyList(),
     val rewardRedemptions: List<@kotlinx.serialization.Contextual RewardRedemption> = emptyList(),
     val trackedApps: List<@kotlinx.serialization.Contextual TrackedApp> = emptyList(),
     val appUsageAggregates: List<@kotlinx.serialization.Contextual AppUsageAggregate> = emptyList(),
     val appUsagePurchases: List<@kotlinx.serialization.Contextual AppUsagePurchase> = emptyList(),
 )
