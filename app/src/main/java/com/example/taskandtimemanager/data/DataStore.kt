package com.example.taskandtimemanager.data

import android.content.Context
import com.example.taskandtimemanager.model.Task
import com.example.taskandtimemanager.model.OneTimeReward
import com.example.taskandtimemanager.model.AppData
import java.time.LocalDateTime

class DataStore(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val rewardDao = db.rewardDao()
    private val appDao = db.appDao()

    // ========== TASKS ==========
    suspend fun getTasks(): List<Task> = taskDao.getAll()

    suspend fun addTask(task: Task) = taskDao.insert(task)

    suspend fun completeTask(taskId: String, reward: Int) {
        val task = taskDao.getAll().find { it.id == taskId }
        task?.let {
            taskDao.update(it.copy(completed = true))
        }
    }

    suspend fun incrementInfiniteTask(taskId: String) {
        val task = taskDao.getAll().find { it.id == taskId }
        task?.let {
            taskDao.update(it.copy(infiniteCount = it.infiniteCount + 1))
        }
    }

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(taskId: String) {
        val task = taskDao.getAll().find { it.id == taskId }
        task?.let { taskDao.delete(it) }
    }

    // ========== REWARDS ==========
    suspend fun getOneTimeRewards(): List<OneTimeReward> = rewardDao.getAll()

    suspend fun addOneTimeReward(reward: OneTimeReward) = rewardDao.insert(reward)

    suspend fun claimOneTimeReward(rewardId: String) {
        val reward = rewardDao.getAll().find { it.id == rewardId }
        reward?.let {
            val now = LocalDateTime.now()
            val updated = it.copy(
                claimedDate = now.toLocalDate().toString(),
                claimedTime = now.toLocalTime().toString()
            )
            rewardDao.update(updated)
        }
    }

    // ========== APPS ==========
    suspend fun getApps(): List<AppData> = appDao.getAll()

    suspend fun addApp(app: AppData) = appDao.insert(app)

    suspend fun setApps(apps: List<AppData>) {
        apps.forEach { appDao.insert(it) }
    }

    suspend fun updateApp(app: AppData) = appDao.update(app)

    // ========== EXPORT ==========
    suspend fun exportToCSV(): String {
        val tasks = taskDao.getAll()
        val rewards = rewardDao.getAll()
        val apps = appDao.getAll()

        val header = "Type,Name,Status,Coins,Date\n"
        val taskLines = tasks.joinToString("\n") { task ->
            "Task,${task.name},${if (task.completed) "Done" else "Pending"},${task.reward},"
        }
        val rewardLines = rewards.filter { it.claimedDate.isNotEmpty() }.joinToString("\n") { reward ->
            "Reward,${reward.name},Claimed,${reward.coins},${reward.claimedDate}"
        }

        return header + taskLines + "\n" + rewardLines
    }
}
