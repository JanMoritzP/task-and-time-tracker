package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.RewardDefinition
import com.example.taskandtimemanager.model.RewardRedemption
import java.time.LocalDateTime
import java.util.UUID

/**
 * Encapsulates operations for reward definitions and reward redemptions.
 */
class RewardManager(
    private val rewardDefinitionDao: RewardDefinitionDao,
    private val rewardRedemptionDao: RewardRedemptionDao,
) {

    suspend fun getRewardDefinitions(): List<RewardDefinition> = rewardDefinitionDao.getAll()

    suspend fun addRewardDefinition(reward: RewardDefinition) = rewardDefinitionDao.insert(reward)

    suspend fun updateRewardDefinition(reward: RewardDefinition) = rewardDefinitionDao.update(reward)

    suspend fun getRewardRedemptions(): List<RewardRedemption> = rewardRedemptionDao.getAll()

    /**
     * Creates a RewardRedemption for the given reward if the provided balance
     * is sufficient to cover its cost.
     *
     * The caller is responsible for supplying an up‑to‑date balance.
     */
    suspend fun redeemRewardIfEnoughCoins(
        rewardId: String,
        currentBalance: Int,
    ): RewardRedemption? {
        val reward = rewardDefinitionDao.getAll().find { it.id == rewardId } ?: return null
        if (currentBalance < reward.coinCost || reward.coinCost <= 0) {
            return null
        }
        val now = LocalDateTime.now()
        val redemption = RewardRedemption(
            id = UUID.randomUUID().toString(),
            rewardDefinitionId = rewardId,
            redemptionDateTime = now.toString(),
            coinsSpent = reward.coinCost,
        )
        rewardRedemptionDao.insert(redemption)
        return redemption
    }
}
