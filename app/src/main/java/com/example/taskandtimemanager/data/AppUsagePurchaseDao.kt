package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.AppUsagePurchase

@Dao
interface AppUsagePurchaseDao {
    @Query("SELECT * FROM app_usage_purchase")
    suspend fun getAll(): List<AppUsagePurchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: AppUsagePurchase)

    @Query("DELETE FROM app_usage_purchase")
    suspend fun deleteAll()

    @Update
    suspend fun update(purchase: AppUsagePurchase)

    @Delete
    suspend fun delete(purchase: AppUsagePurchase)
}
