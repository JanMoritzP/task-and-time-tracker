package com.example.taskandtimemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.RewardDefinition
import com.example.taskandtimemanager.model.RewardRedemption
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import com.example.taskandtimemanager.model.TrackedApp

@Database(
    entities = [
        TaskDefinition::class,
        TaskExecution::class,
        RewardDefinition::class,
        RewardRedemption::class,
        TrackedApp::class,
        AppUsageAggregate::class,
        AppUsagePurchase::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDefinitionDao(): TaskDefinitionDao

    abstract fun taskExecutionDao(): TaskExecutionDao

    abstract fun rewardDefinitionDao(): RewardDefinitionDao

    abstract fun rewardRedemptionDao(): RewardRedemptionDao

    abstract fun trackedAppDao(): TrackedAppDao

    abstract fun appUsageAggregateDao(): AppUsageAggregateDao

    abstract fun appUsagePurchaseDao(): AppUsagePurchaseDao

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
