package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.AppData

@Dao
interface AppDataDao {
    @Query("SELECT * FROM app_data")
    suspend fun getAll(): List<AppData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppData)

    @Update
    suspend fun update(app: AppData)

    @Delete
    suspend fun delete(app: AppData)
}
