package com.example.fitnesstrackerapp.tracking.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // e.g., "Running", "Cycling", "General Workout"
    val timestamp: Long, // start time of the activity, in milliseconds
    val durationMillis: Long, // duration of the activity, in milliseconds
    val caloriesBurned: Int, // estimated calories burned
    val notes: String?, // optional notes from the user
    val pathPoints: List<String> = emptyList() // List of "latitude,longitude" strings
)
