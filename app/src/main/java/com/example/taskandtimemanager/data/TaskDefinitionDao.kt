package com.example.taskandtimemanager.data

import androidx.room.*
import com.example.taskandtimemanager.model.TaskDefinition

@Dao
interface TaskDefinitionDao {
    @Query("SELECT * FROM task_definition")
    suspend fun getAll(): List<TaskDefinition>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskDefinition)

    @Query("DELETE FROM task_definition")
    suspend fun deleteAll()

    @Update
    suspend fun update(task: TaskDefinition)

    @Delete
    suspend fun delete(task: TaskDefinition)
}
