package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.RewardRedemption
import com.example.taskandtimemanager.model.TaskExecution

/**
 * Computes the global coin balance from task executions, reward redemptions
 * and app usage purchases.
 */
class CoinManager(
    private val taskExecutionDao: TaskExecutionDao,
    private val rewardRedemptionDao: RewardRedemptionDao,
    private val appUsagePurchaseDao: AppUsagePurchaseDao,
) {

    /**
     * Returns the current coin balance derived from all history.
     */
    suspend fun getCoinBalance(): Int = recalculateCoinBalance()

    /**
     * Recomputes the coin balance from scratch:
     *  - Sum of TaskExecution.coinsAwarded
     *  - Minus sum of RewardRedemption.coinsSpent
     *  - Minus sum of AppUsagePurchase.coinsSpent
     */
    suspend fun recalculateCoinBalance(): Int {
        val executions: List<TaskExecution> = taskExecutionDao.getAll()
        val redemptions: List<RewardRedemption> = rewardRedemptionDao.getAll()
        val purchases: List<AppUsagePurchase> = appUsagePurchaseDao.getAll()

        val coinsFromTasks = executions.sumOf { it.coinsAwarded }
        val coinsSpentOnRewards = redemptions.sumOf { it.coinsSpent }
        val coinsSpentOnAppTime = purchases.sumOf { it.coinsSpent }

        return coinsFromTasks - coinsSpentOnRewards - coinsSpentOnAppTime
    }
}
