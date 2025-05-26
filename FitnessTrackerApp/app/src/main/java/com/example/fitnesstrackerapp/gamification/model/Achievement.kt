package com.example.fitnesstrackerapp.gamification.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String, // e.g., "FIRST_RUN", "RUN_5KM", "EARLY_BIRD"
    val name: String, // e.g., "First Run"
    val description: String, // e.g., "Log your first running activity"
    val iconName: String, // placeholder for icon resource name, e.g., "ic_achievement_first_run"
    val targetValue: Double, // e.g., 1.0 for first run, 5.0 for 5km, 7.0 for 7 AM
    val type: String // enum or const val: "FIRST_ACTIVITY_TYPE", "DISTANCE_SINGLE_ACTIVITY", "TIME_OF_DAY_ACTIVITY_LOGGED", "TOTAL_ACTIVITIES_LOGGED", "TOTAL_DISTANCE_TYPE", "TOTAL_ACTIVITIES_WINDOW"
)

// Example Achievement Types (could be an enum or sealed class for more type safety if preferred)
object AchievementTypes {
    const val FIRST_ACTIVITY_TYPE = "FIRST_ACTIVITY_TYPE" // Target: 1.0, checks specific activity type
    const val DISTANCE_SINGLE_ACTIVITY = "DISTANCE_SINGLE_ACTIVITY" // Target: distance in km, checks specific activity type
    const val TIME_OF_DAY_ACTIVITY_LOGGED = "TIME_OF_DAY_ACTIVITY_LOGGED" // Target: hour (e.g., 7.0 for before 7 AM)
    const val TOTAL_ACTIVITIES_LOGGED = "TOTAL_ACTIVITIES_LOGGED" // Target: count of all activities
    const val TOTAL_DISTANCE_TYPE = "TOTAL_DISTANCE_TYPE" // Target: total distance in km for a specific activity type
    const val TOTAL_ACTIVITIES_WINDOW = "TOTAL_ACTIVITIES_WINDOW" // Target: count of activities within a time window (e.g., 7 days)
}
