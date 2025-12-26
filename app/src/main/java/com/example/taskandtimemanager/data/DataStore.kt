package com.example.taskandtimemanager.data

import android.content.Context
import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.RewardDefinition
import com.example.taskandtimemanager.model.RewardRedemption
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import com.example.taskandtimemanager.model.TrackedApp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore is a façade over the Room database that exposes high‑level domain
 * operations used by the UI and background services.
 *
 * It delegates to feature‑specific manager classes to keep responsibilities focused
 * and respects the one‑class‑per‑file rule for all helpers.
 */
class DataStore(context: Context) {

    private val db = AppDatabase.getDatabase(context)

    // Feature specific managers
    private val taskManager = TaskManager(db.taskDefinitionDao(), db.taskExecutionDao())
    private val rewardManager = RewardManager(db.rewardDefinitionDao(), db.rewardRedemptionDao())
    private val appUsageManager = AppUsageManager(
        db.trackedAppDao(),
        db.appUsageAggregateDao(),
        db.appUsagePurchaseDao(),
    )

    private val coinManager = CoinManager(
        taskExecutionDao = db.taskExecutionDao(),
        rewardRedemptionDao = db.rewardRedemptionDao(),
        appUsagePurchaseDao = db.appUsagePurchaseDao(),
    )

    // ========= TASKS =========

    suspend fun getTaskDefinitions(): List<TaskDefinition> = taskManager.getTaskDefinitions()

    suspend fun addTaskDefinition(task: TaskDefinition) = taskManager.addTaskDefinition(task)

    suspend fun updateTaskDefinition(task: TaskDefinition) = taskManager.updateTaskDefinition(task)

    /**
     * Archives the given task definition (preferred over hard delete).
     */
    suspend fun archiveTaskDefinition(taskId: String) = taskManager.archiveTaskDefinition(taskId)

    suspend fun getExecutionsForDate(date: LocalDate): List<TaskExecution> =
        taskManager.getExecutionsForDate(date)

    suspend fun addTaskExecution(execution: TaskExecution) = taskManager.addTaskExecution(execution)

    /**
     * Convenience wrapper for creating a DONE execution for "now" using the
     * task's configured reward coins.
     */
    suspend fun completeTaskNow(taskId: String): TaskExecution = taskManager.completeTaskNow(taskId)

    // ========= REWARDS =========

    suspend fun getRewardDefinitions(): List<RewardDefinition> = rewardManager.getRewardDefinitions()

    suspend fun addRewardDefinition(reward: RewardDefinition) = rewardManager.addRewardDefinition(reward)

    suspend fun updateRewardDefinition(reward: RewardDefinition) = rewardManager.updateRewardDefinition(reward)

    suspend fun getRewardRedemptions(): List<RewardRedemption> = rewardManager.getRewardRedemptions()

    /**
     * Redeem a reward by id if there are enough coins according to the derived
     * global balance. Returns the created redemption or null when insufficient coins.
     */
    suspend fun redeemReward(rewardId: String): RewardRedemption? {
        val balance = coinManager.getCoinBalance()
        return rewardManager.redeemRewardIfEnoughCoins(rewardId, balance)
    }

    // ========= APPS / USAGE =========

    suspend fun getTrackedApps(): List<TrackedApp> = appUsageManager.getTrackedApps()

    suspend fun addTrackedApp(app: TrackedApp) = appUsageManager.addTrackedApp(app)

    suspend fun updateTrackedApp(app: TrackedApp) = appUsageManager.updateTrackedApp(app)

    suspend fun getAppUsageAggregatesForDate(date: LocalDate): List<AppUsageAggregate> =
        appUsageManager.getAppUsageAggregatesForDate(date)

    suspend fun addOrUpdateAppUsageAggregate(aggregate: AppUsageAggregate) =
        appUsageManager.addOrUpdateAppUsageAggregate(aggregate)

    suspend fun addAppUsagePurchase(purchase: AppUsagePurchase) =
        appUsageManager.addAppUsagePurchase(purchase)

    suspend fun getAppUsagePurchases(appId: String): List<AppUsagePurchase> =
        appUsageManager.getAppUsagePurchases(appId)

    /**
     * Helper used by UI when the user wants to buy additional minutes for an app.
     * The method checks against the global coin balance and only inserts a purchase
     * if enough coins are available.
     */
    suspend fun buyAppTimeIfEnoughCoins(appId: String, minutesToBuy: Long): AppUsagePurchase? {
        val apps = appUsageManager.getTrackedApps()
        val app = apps.find { it.id == appId } ?: return null

        val costFloat = app.costPerMinute * minutesToBuy
        val coinsRequired = costFloat.toInt()
        val currentBalance = coinManager.getCoinBalance()
        if (coinsRequired <= 0 || currentBalance < coinsRequired) {
            return null
        }

        val now = LocalDateTime.now()
        val purchase = AppUsagePurchase(
            id = UUID.randomUUID().toString(),
            appId = appId,
            minutesPurchased = minutesToBuy,
            coinsSpent = coinsRequired,
            purchaseDateTime = now.toString(),
        )
        appUsageManager.addAppUsagePurchase(purchase)
        return purchase
    }

    // ========= COINS / BALANCE =========

    suspend fun getCoinBalance(): Int = coinManager.getCoinBalance()

    suspend fun recalculateCoinBalance(): Int = coinManager.recalculateCoinBalance()
 
     // ========= EXPORT / IMPORT =========
 
     /**
      * Export the complete logical state of the application as a single JSON
      * document. The format is defined by [ExportImportState] and is stable
      * enough for round‑trips between installs on the same or different
      * devices.
      */
     suspend fun exportState(): String {
         val state = ExportImportState(
             taskDefinitions = db.taskDefinitionDao().getAll(),
             taskExecutions = db.taskExecutionDao().getAll(),
             rewardDefinitions = db.rewardDefinitionDao().getAll(),
             rewardRedemptions = db.rewardRedemptionDao().getAll(),
             trackedApps = db.trackedAppDao().getAll(),
             appUsageAggregates = db.appUsageAggregateDao().getAll(),
             appUsagePurchases = db.appUsagePurchaseDao().getAll(),
         )
         return Json.encodeToString(state)
     }
 
     /**
      * Import a previously exported JSON document produced by [exportState].
      *
      * Strategy: clear all existing data in the managed tables and then insert
      * all records from the imported snapshot.
      */
     suspend fun importState(serialized: String) {
         val json = serialized.trim()
         if (json.isEmpty()) return
 
         val state = try {
             Json.decodeFromString(ExportImportState.serializer(), json)
         } catch (e: Exception) {
             // Parsing failed – surface as an IllegalArgumentException so the
             // UI can display an appropriate error message.
             throw IllegalArgumentException("Failed to parse import data", e)
         }
 
         // Clear all managed tables before inserting imported data to avoid
         // id collisions and mixed states from previous usage.
         db.taskExecutionDao().deleteAll()
         db.taskDefinitionDao().deleteAll()
         db.rewardRedemptionDao().deleteAll()
         db.rewardDefinitionDao().deleteAll()
         db.appUsagePurchaseDao().deleteAll()
         db.appUsageAggregateDao().deleteAll()
         db.trackedAppDao().deleteAll()
 
         // Re‑insert in dependency‑friendly order.
         state.taskDefinitions.forEach { db.taskDefinitionDao().insert(it) }
         state.taskExecutions.forEach { db.taskExecutionDao().insert(it) }
         state.rewardDefinitions.forEach { db.rewardDefinitionDao().insert(it) }
         state.rewardRedemptions.forEach { db.rewardRedemptionDao().insert(it) }
         state.trackedApps.forEach { db.trackedAppDao().insert(it) }
         state.appUsageAggregates.forEach { db.appUsageAggregateDao().insert(it) }
         state.appUsagePurchases.forEach { db.appUsagePurchaseDao().insert(it) }
     }
 }
