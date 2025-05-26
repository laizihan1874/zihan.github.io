package com.example.fitnesstrackerapp.user.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String, // Firebase UID
    val displayName: String?,
    val email: String?,
    val xpPoints: Long = 0L,
    val level: Int = 1,

    // Fitness Assessment Fields
    val fitnessLevel: String? = null, // e.g., "beginner", "intermediate", "advanced"
    val primaryGoals: List<String>? = null, // e.g., ["weight_loss", "endurance"]
    val preferredActivities: List<String>? = null, // e.g., ["running", "cycling"]
    val age: Int? = null,
    val gender: String? = null, // e.g., "male", "female", "other", "prefer_not_to_say"
    val weightKg: Float? = null,
    val heightCm: Float? = null
)
