package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.TrackedApp

@Dao
interface TrackedAppDao {
    @Query("SELECT * FROM tracked_app")
    suspend fun getAll(): List<TrackedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: TrackedApp)

    @Query("DELETE FROM tracked_app")
    suspend fun deleteAll()

    @Update
    suspend fun update(app: TrackedApp)

    @Delete
    suspend fun delete(app: TrackedApp)
}
