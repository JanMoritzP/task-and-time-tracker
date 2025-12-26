package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.RewardDefinition

@Dao
interface RewardDefinitionDao {
    @Query("SELECT * FROM reward_definition")
    suspend fun getAll(): List<RewardDefinition>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: RewardDefinition)

    @Query("DELETE FROM reward_definition")
    suspend fun deleteAll()

    @Update
    suspend fun update(reward: RewardDefinition)

    @Delete
    suspend fun delete(reward: RewardDefinition)
}
