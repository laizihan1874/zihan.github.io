package com.example.fitnesstrackerapp.challenge.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey
    val id: String, // e.g., "DAILY_STEPS_5K", "WEEKLY_RUN_15KM"
    val name: String, // e.g., "Daily 5,000 Steps Challenge"
    val description: String,
    val challengeType: String, // See ChallengeTypes object
    val activityTypeFilter: String? = null, // e.g., "Running (GPS)" for a distance run challenge
    val targetValue: Double,
    val durationDays: Int, // e.g., 1 for daily, 7 for weekly
    val xpReward: Int,
    val isActiveGlobally: Boolean = true // For admin control later
)

object ChallengeTypes {
    const val STEPS = "STEPS" // Target: number of steps
    const val ACTIVE_MINUTES = "ACTIVE_MINUTES" // Target: total minutes of any/filtered activity
    const val DISTANCE_KM = "DISTANCE_KM" // Target: distance in KM for a filtered activity
    const val LOG_ACTIVITY_COUNT = "LOG_ACTIVITY_COUNT" // Target: count of specific/any activity type logged
    // Could add: CALORIES_BURNED, etc.
}
