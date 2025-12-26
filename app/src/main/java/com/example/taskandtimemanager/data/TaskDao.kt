package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    suspend fun getAll(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM task")
    suspend fun deleteAll()
}
