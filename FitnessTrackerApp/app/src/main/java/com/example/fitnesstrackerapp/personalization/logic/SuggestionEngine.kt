package com.example.fitnesstrackerapp.personalization.logic

import android.location.Location
import com.example.fitnesstrackerapp.gamification.logic.AchievementChecker // For RUNNING_GPS_TYPE
import com.example.fitnesstrackerapp.goal.model.UserGoalDao
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.ActivityLogDao
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import java.util.concurrent.TimeUnit

class SuggestionEngine(
    private val userProfileDao: UserProfileDao,
    private val activityLogDao: ActivityLogDao,
    private val userGoalDao: UserGoalDao // Optional for now, can be used later
) {

    companion object {
        private const val RECENT_ACTIVITY_LIMIT = 5
        private const val STRENUOUS_RUN_DISTANCE_KM = 10.0
        private const val STRENUOUS_DURATION_MINS = 60.0
    }

    suspend fun getWorkoutSuggestion(userId: String): String? {
        val userProfile = userProfileDao.getUserProfileSuspending(userId)
        val recentActivities = activityLogDao.getRecentActivitiesForUser(userId, RECENT_ACTIVITY_LIMIT)

        // Rule 1: Rest Day Suggestion
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.DAYS.toMillis(1)

        for (activity in recentActivities) {
            if (activity.timestamp >= yesterday) { // Activity logged today or yesterday
                var isStrenuous = false
                if (activity.type == AchievementChecker.RUNNING_GPS_TYPE) {
                    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(activity.durationMillis).toDouble()
                    if (durationMinutes > STRENUOUS_DURATION_MINS) {
                        isStrenuous = true
                    } else if (activity.pathPoints.isNotEmpty()) {
                        var distanceMeters = 0f
                        val latLngList = activity.pathPoints.mapNotNull {
                            val parts = it.split(",")
                            if (parts.size == 2) Pair(parts[0].toDouble(), parts[1].toDouble()) else null
                        }
                        for (i in 0 until latLngList.size - 1) {
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                latLngList[i].first, latLngList[i].second,
                                latLngList[i+1].first, latLngList[i+1].second,
                                results
                            )
                            distanceMeters += results[0]
                        }
                        if ((distanceMeters / 1000.0) >= STRENUOUS_RUN_DISTANCE_KM) {
                            isStrenuous = true
                        }
                    }
                }
                // Could add other strenuous types here, e.g., long cycling
                if (isStrenuous) {
                    return "You had a great workout recently! Consider a rest day or some light stretching today."
                }
            }
        }

        // Rule 2: Preferred Activity Nudge
        userProfile?.preferredActivities?.firstOrNull()?.let { preferredActivity ->
            val threeDaysAgo = now - TimeUnit.DAYS.toMillis(3)
            val hasDonePreferredRecently = recentActivities.any {
                it.type.equals(preferredActivity, ignoreCase = true) && it.timestamp >= threeDaysAgo
            }
            if (!hasDonePreferredRecently) {
                return "How about a ${preferredActivity.replaceFirstChar { it.uppercase() }} session today? Even 20-30 minutes would be great!"
            }
        }

        // Rule 3: Beginner Frequency
        if (userProfile?.fitnessLevel == "beginner") {
            val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
            val hasDoneAnyActivityInLastTwoDays = recentActivities.any { it.timestamp >= twoDaysAgo }
            if (!hasDoneAnyActivityInLastTwoDays) {
                return "Consistency is key! Try a short walk or any activity you enjoy for 15-20 minutes today."
            }
        }
        
        // Default/Fallback Suggestion
        userProfile?.preferredActivities?.firstOrNull()?.let {
            return "Stay active today! Try a ${it.replaceFirstChar { it.uppercase() }} session or another activity you enjoy."
        }
        
        return "Time for a workout! How about a walk or a quick run?" // Generic fallback
    }
}
