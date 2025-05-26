package com.example.fitnesstrackerapp.gamification.logic

import android.location.Location
import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao

class XPCalculator(
    private val userProfileDao: UserProfileDao,
    private val levelingSystem: LevelingSystem
) {

    private suspend fun ensureUserProfileExists(userId: String, displayName: String?, email: String?): UserProfile {
        var userProfile = userProfileDao.getUserProfileSuspending(userId)
        if (userProfile == null) {
            userProfile = UserProfile(
                userId = userId,
                displayName = displayName,
                email = email,
                xpPoints = 0L,
                level = 1
            )
            userProfileDao.upsertUserProfile(userProfile)
        } else if (userProfile.displayName != displayName || userProfile.email != email) {
            // Optionally update display name and email if they have changed since last time
            userProfile = userProfile.copy(displayName = displayName, email = email)
            userProfileDao.upsertUserProfile(userProfile)
        }
        return userProfile
    }

    suspend fun addXPForActivity(userId: String, activityLog: ActivityLog, displayName: String?, email: String?) {
        val userProfile = ensureUserProfileExists(userId, displayName, email)
        var activityXP = 0L
        val durationMinutes = activityLog.durationMillis / (1000.0 * 60.0)

        when (activityLog.type) {
            AchievementChecker.RUNNING_GPS_TYPE -> { // "Running (GPS)"
                if (activityLog.pathPoints.isNotEmpty()) {
                    val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                    activityXP = (distanceKm * levelingSystem.XP_PER_KM_RUNNING_GPS).toLong()
                } else { // Fallback if somehow "Running (GPS)" has no path points
                    activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_WALKING).toLong() // Approx with walking per minute
                }
            }
            "Cycling (GPS)" -> {
                if (activityLog.pathPoints.isNotEmpty()) {
                    val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                    activityXP = (distanceKm * levelingSystem.XP_PER_KM_CYCLING_GPS).toLong()
                } else {
                     activityXP = (durationMinutes * (levelingSystem.XP_PER_KM_CYCLING_GPS / 2.0) ).toLong() // Rough estimate
                }
            }
            "Walking" -> {
                if (activityLog.pathPoints.isNotEmpty()) {
                    val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                    activityXP = (distanceKm * levelingSystem.XP_PER_KM_WALKING).toLong()
                } else {
                    activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_WALKING).toLong()
                }
            }
            "Hiking" -> {
                if (activityLog.pathPoints.isNotEmpty()) {
                    val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                    activityXP = (distanceKm * levelingSystem.XP_PER_KM_HIKING).toLong()
                } else {
                    activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_HIKING).toLong()
                }
            }
            "Swimming (Pool)" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_SWIMMING).toLong()
            }
            "Weight Training" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_WEIGHT_TRAINING).toLong()
            }
            "Yoga" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_YOGA).toLong()
            }
            "HIIT" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_HIIT).toLong()
            }
            "Pilates" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_PILATES).toLong()
            }
            "Team Sport (General)" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_TEAM_SPORT).toLong()
            }
            "Dancing" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_DANCING).toLong()
            }
            "Martial Arts" -> {
                activityXP = (durationMinutes * levelingSystem.XP_PER_MINUTE_MARTIAL_ARTS).toLong()
            }
            "General Workout", "Other" -> {
                activityXP = levelingSystem.XP_PER_GENERAL_ACTIVITY_LOGGED
            }
            else -> { // Fallback for any other unrecognized types
                activityXP = levelingSystem.XP_PER_GENERAL_ACTIVITY_LOGGED 
            }
        }

        if (activityXP > 0) {
            val newTotalXP = userProfile.xpPoints + activityXP
            val newLevel = levelingSystem.getLevelForXP(newTotalXP)
            userProfileDao.updateUserXPAndLevel(userId, newTotalXP, newLevel)
            // TODO: Log level up or prepare event for UI (e.g., Toast "Leveled Up to Level $newLevel!")
        }
    }

    suspend fun addXPForAchievement(userId: String, achievement: Achievement, displayName: String?, email: String?) {
        val userProfile = ensureUserProfileExists(userId, displayName, email)
        val achievementXP = levelingSystem.XP_PER_ACHIEVEMENT_UNLOCKED

        if (achievementXP > 0) {
            val newTotalXP = userProfile.xpPoints + achievementXP
            val newLevel = levelingSystem.getLevelForXP(newTotalXP)
            userProfileDao.updateUserXPAndLevel(userId, newTotalXP, newLevel)
            // TODO: Log level up or prepare event for UI
        }
    }

    suspend fun addXP(userId: String, xpAmount: Int, userName: String?, email: String?) {
        if (xpAmount <= 0) return // No XP to add

        val userProfile = ensureUserProfileExists(userId, userName, email)
        val newTotalXP = userProfile.xpPoints + xpAmount
        val newLevel = levelingSystem.getLevelForXP(newTotalXP)

        if (newLevel > userProfile.level) {
            // TODO: Handle Level Up event (e.g., show Toast, notification, specific UI change)
            // Log.d("XPCalculator", "User $userId leveled up to Level $newLevel!")
        }
        userProfileDao.updateUserXPAndLevel(userId, newTotalXP, newLevel)
    }

    // Helper function to calculate distance from path points
    private fun calculateDistanceKmFromPath(pathPoints: List<String>): Double {
        if (pathPoints.size < 2) return 0.0
        var totalDistanceMeters = 0f
        val latLngList = pathPoints.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                try { Pair(parts[0].toDouble(), parts[1].toDouble()) }
                catch (e: NumberFormatException) { null }
            } else null
        }
        for (i in 0 until latLngList.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                latLngList[i].first, latLngList[i].second,
                latLngList[i+1].first, latLngList[i+1].second,
                results
            )
            totalDistanceMeters += results[0]
        }
        return totalDistanceMeters / 1000.0
    }
}
