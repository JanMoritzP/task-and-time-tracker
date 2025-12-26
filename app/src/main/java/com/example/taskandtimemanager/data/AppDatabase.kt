package com.example.taskandtimemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taskandtimemanager.model.Task
import com.example.taskandtimemanager.model.OneTimeReward
import com.example.taskandtimemanager.model.AppData

@Database(entities = [Task::class, OneTimeReward::class, AppData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun rewardDao(): RewardDao
    abstract fun appDao(): AppDataDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "taskandtimemanager.db")
                    .build()
                    .also { Instance = it }
            }
    }
}
