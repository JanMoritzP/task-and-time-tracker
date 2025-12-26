package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.AppUsageAggregate

@Dao
interface AppUsageAggregateDao {
    @Query("SELECT * FROM app_usage_aggregate")
    suspend fun getAll(): List<AppUsageAggregate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aggregate: AppUsageAggregate)

    @Query("DELETE FROM app_usage_aggregate")
    suspend fun deleteAll()

    @Update
    suspend fun update(aggregate: AppUsageAggregate)

    @Delete
    suspend fun delete(aggregate: AppUsageAggregate)
}
