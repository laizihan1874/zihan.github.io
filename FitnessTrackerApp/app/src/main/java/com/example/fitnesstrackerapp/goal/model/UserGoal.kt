package com.example.fitnesstrackerapp.goal.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(index = true)
    val userId: String, // Firebase UID

    val goalType: String, // e.g., "WEIGHT_TARGET", "WEEKLY_DISTANCE_RUN"
    val targetValue: Double,
    var currentValue: Double = 0.0,
    val startDate: Long, // Timestamp
    val targetDate: Long? = null, // Nullable timestamp for goals with a deadline
    var lastUpdated: Long, // Timestamp of last progress update
    var isActive: Boolean = true,
    var isCompleted: Boolean = false,
    val activityTypeFilter: String? = null // e.g., "Running (GPS)"
)
