package com.example.taskandtimemanager.data

import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Encapsulates task-related operations on TaskDefinition and TaskExecution.
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
     * Creates an execution for the current date/time.
     *
     * If the task defines a recurringRewardCoins value and has already been completed
     * today, subsequent completions will use recurringRewardCoins instead of rewardCoins.
     *
     * When [skipped] is true, status will be "SKIPPED" and no coins are awarded.
     */
    suspend fun completeTaskNow(taskId: String, skipped: Boolean = false): TaskExecution {
        val task = taskDefinitionDao.getAll().find { it.id == taskId }
            ?: error("TaskDefinition not found for id=$taskId")
        val now = LocalDateTime.now()
        val today = now.toLocalDate().toString()

        val executionsToday = taskExecutionDao.getAll()
            .filter { it.taskDefinitionId == taskId && it.date == today && it.status == "DONE" }

        val coinsForThisExecution = if (skipped) {
            0
        } else if (task.recurringRewardCoins != null && executionsToday.isNotEmpty()) {
            task.recurringRewardCoins
        } else {
            task.rewardCoins
        }

        val status = if (skipped) "SKIPPED" else "DONE"

        val execution = TaskExecution(
            id = UUID.randomUUID().toString(),
            taskDefinitionId = taskId,
            date = today,
            time = now.toLocalTime().toString(),
            status = status,
            coinsAwarded = coinsForThisExecution,
        )
        taskExecutionDao.insert(execution)
        return execution
    }
}
