package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.OneTimeReward

@Dao
interface RewardDao {
    @Query("SELECT * FROM one_time_reward")
    suspend fun getAll(): List<OneTimeReward>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: OneTimeReward)

    @Update
    suspend fun update(reward: OneTimeReward)

    @Delete
    suspend fun delete(reward: OneTimeReward)
}
