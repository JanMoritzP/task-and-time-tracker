package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.TaskExecution

@Dao
interface TaskExecutionDao {
    @Query("SELECT * FROM task_execution")
    suspend fun getAll(): List<TaskExecution>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(execution: TaskExecution)

    @Query("DELETE FROM task_execution")
    suspend fun deleteAll()

    @Update
    suspend fun update(execution: TaskExecution)

    @Delete
    suspend fun delete(execution: TaskExecution)
}
