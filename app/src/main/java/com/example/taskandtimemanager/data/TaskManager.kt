package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Encapsulates task‑related operations on TaskDefinition and TaskExecution.
 */
class TaskManager(
    private val taskDefinitionDao: TaskDefinitionDao,
    private val taskExecutionDao: TaskExecutionDao,
) {

    suspend fun getTaskDefinitions(): List<TaskDefinition> = taskDefinitionDao.getAll()

    suspend fun addTaskDefinition(task: TaskDefinition) = taskDefinitionDao.insert(task)

    suspend fun updateTaskDefinition(task: TaskDefinition) = taskDefinitionDao.update(task)

    /**
     * Preferred delete strategy: mark the task as archived so that history is preserved.
     */
    suspend fun archiveTaskDefinition(taskId: String) {
        val task = taskDefinitionDao.getAll().find { it.id == taskId } ?: return
        val archived = task.copy(archived = true)
        taskDefinitionDao.update(archived)
    }

    suspend fun getExecutionsForDate(date: LocalDate): List<TaskExecution> =
        taskExecutionDao.getAll().filter { it.date == date.toString() }

    suspend fun addTaskExecution(execution: TaskExecution) {
        taskExecutionDao.insert(execution)
    }

    /**
     * Creates a DONE execution for the current date/time using the rewardCoins
     * value configured on the TaskDefinition.
     */
    suspend fun completeTaskNow(taskId: String): TaskExecution {
        val task = taskDefinitionDao.getAll().find { it.id == taskId }
            ?: error("TaskDefinition not found for id=$taskId")
        val now = LocalDateTime.now()
        val execution = TaskExecution(
            id = UUID.randomUUID().toString(),
            taskDefinitionId = taskId,
            date = now.toLocalDate().toString(),
            time = now.toLocalTime().toString(),
            status = "DONE",
            coinsAwarded = task.rewardCoins,
        )
        taskExecutionDao.insert(execution)
        return execution
    }
}
