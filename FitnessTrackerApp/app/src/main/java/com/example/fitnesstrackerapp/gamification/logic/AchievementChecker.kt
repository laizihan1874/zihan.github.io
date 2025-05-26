package com.example.fitnesstrackerapp.gamification.logic

import android.content.Context
import android.widget.Toast
import com.example.fitnesstrackerapp.gamification.model.AchievementDao
import com.example.fitnesstrackerapp.gamification.model.UserAchievement
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.ActivityLogDao
import com.example.fitnesstrackerapp.user.model.UserProfileDao // Added
import com.google.firebase.auth.FirebaseAuth // Added
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AchievementChecker(
    private val achievementDao: AchievementDao,
    private val activityLogDao: ActivityLogDao,
    private val userProfileDao: UserProfileDao, // Added
    private val xpCalculator: XPCalculator,   // Added
    private val context: Context
) {

    companion object {
        const val FIRST_RUN_ID = "FIRST_RUN"
        const val RUNNING_GPS_TYPE = "Running (GPS)" // Consistent type name
    }

    suspend fun checkAchievements(userId: String, newActivityLog: ActivityLog) {
        // --- 1. Check for "FIRST_RUN" Achievement ---
        if (newActivityLog.type == RUNNING_GPS_TYPE) {
            val existingFirstRun = achievementDao.getSpecificUserAchievement(userId, FIRST_RUN_ID)
            if (existingFirstRun == null) {
                val unlocked = achievementDao.unlockAchievement(
                    UserAchievement(
                        userId = userId,
                        achievementId = FIRST_RUN_ID,
                        unlockedTimestamp = System.currentTimeMillis()
                    )
                )
                if (unlocked != -1L) { // -1L indicates conflict/ignore, so anything else means success
                    val achievementDetails = achievementDao.getAchievementById(FIRST_RUN_ID)
                    if (achievementDetails != null) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        xpCalculator.addXPForAchievement(
                            userId,
                            achievementDetails,
                            currentUser?.displayName,
                            currentUser?.email
                        )
                        withContext(Dispatchers.Main) { // Switch to Main thread for Toast
                            Toast.makeText(
                                context,
                                "Achievement Unlocked: ${achievementDetails.name}!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // --- 2. Check for "RUN_5KM" Achievement ---
        // TODO: Implement logic if newActivityLog.type == RUNNING_GPS_TYPE and distance (from pathPoints) >= 5km

        // --- 3. Check for "EARLY_BIRD" ---
        // TODO: Implement logic based on newActivityLog.timestamp (extract hour)

        // --- 4. Check for "WEEKLY_WARRIOR" ---
        // TODO: This would require querying activityLogDao for activities in the last 7 days for this user.

        // ... other achievement checks will follow similar patterns ...
    }
}
