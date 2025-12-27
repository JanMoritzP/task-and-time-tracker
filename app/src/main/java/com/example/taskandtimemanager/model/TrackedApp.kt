package com.example.taskandtimemanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_app")
data class TrackedApp(
    @PrimaryKey
    val id: String,
    val name: String,
    val packageName: String,
    val costPerMinute: Float = 0f,
    val purchasedMinutesTotal: Long = 0L,
    val isBlocked: Boolean = false,
    /**
     * When true, this app is exempt from blocking checks until the next
     * nightly cutoff (06:00 local time). [nightOverrideActivatedAt] records
     * when the override was turned on so we can distinguish between
     * overrides that started before/after the daily cutoff.
     */
    val nightOverrideEnabled: Boolean = false,
    /** ISO-8601 timestamp of when the night override was last activated. */
    val nightOverrideActivatedAt: String? = null,
)
