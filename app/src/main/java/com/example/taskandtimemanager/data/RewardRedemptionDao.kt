package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.RewardRedemption

@Dao
interface RewardRedemptionDao {
    @Query("SELECT * FROM reward_redemption")
    suspend fun getAll(): List<RewardRedemption>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rewardRedemption: RewardRedemption)

    @Query("DELETE FROM reward_redemption")
    suspend fun deleteAll()

    @Update
    suspend fun update(rewardRedemption: RewardRedemption)

    @Delete
    suspend fun delete(rewardRedemption: RewardRedemption)
}
