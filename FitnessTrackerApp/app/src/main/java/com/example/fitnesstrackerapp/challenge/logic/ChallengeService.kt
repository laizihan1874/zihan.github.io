package com.example.fitnesstrackerapp.challenge.logic

import android.content.Context // For potential Toasts, though better in ViewModel
import android.location.Location
import android.util.Log
import com.example.fitnesstrackerapp.challenge.model.Challenge
import com.example.fitnesstrackerapp.challenge.model.ChallengeDao
import com.example.fitnesstrackerapp.challenge.model.ChallengeTypes
import com.example.fitnesstrackerapp.challenge.model.UserChallenge
import com.example.fitnesstrackerapp.gamification.logic.AchievementChecker // For RUNNING_GPS_TYPE
import com.example.fitnesstrackerapp.gamification.logic.XPCalculator
import com.example.fitnesstrackerapp.social.repository.Result // Using existing Result class
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import java.util.concurrent.TimeUnit

class ChallengeService(
    private val challengeDao: ChallengeDao,
    private val userProfileDao: UserProfileDao, // For fetching user details for XPCalculator
    private val xpCalculator: XPCalculator,
    private val context: Context // For Toasts if needed directly, or pass messages up
) {
    companion object {
        private const val TAG = "ChallengeService"
        const val DAY_IN_MS = 24 * 60 * 60 * 1000L
    }

    suspend fun joinChallenge(userId: String, challenge: Challenge): Result<UserChallenge> {
        val currentTime = System.currentTimeMillis()
        val existingActiveInstance = challengeDao.getActiveUserChallengeInstance(userId, challenge.id, currentTime)

        if (existingActiveInstance != null) {
            return Result.Error(Exception("Already joined or have an active instance of this challenge."))
        }

        val startDate = currentTime
        val endDate = startDate + (challenge.durationDays * DAY_IN_MS)
        val newUserChallenge = UserChallenge(
            userId = userId,
            challengeId = challenge.id,
            startDate = startDate,
            endDate = endDate,
            currentProgress = 0.0,
            isCompleted = false,
            isRewardClaimed = false
        )

        return try {
            val newId = challengeDao.joinChallenge(newUserChallenge)
            if (newId != -1L) { // -1L indicates IGNORE conflict strategy didn't insert
                Result.Success(newUserChallenge.copy(id = newId))
            } else {
                 // This case might happen if a non-active (expired but not completed) one exists and IGNORE prevents insert.
                 // Or if somehow getActiveUserChallengeInstance missed it.
                 // For simplicity, if IGNORE prevents insert, assume it's a re-join attempt of an identical (non-active) entry.
                Result.Error(Exception("Failed to join challenge. May already exist in a non-active state or recently joined."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error joining challenge ${challenge.id} for user $userId", e)
            Result.Error(e)
        }
    }
    
    private suspend fun getChallengeDetails(challengeId: String): Challenge? {
        // This function is needed if not passing the full Challenge object around.
        // Assuming ChallengeDao has getChallengeById(challengeId: String): Challenge?
        // For now, the caller of updateChallengeProgress might need to provide it, or we fetch all global challenges once.
        // Let's assume for now that `updateChallengeProgressOnActivityLogged` will fetch it.
        // This is a placeholder, actual implementation might vary based on ChallengeDao.
        // For now, let's assume `challengeDao.getChallengeById(challengeId)` exists (it's in the DAO plan)
        return challengeDao.getChallengeById(challengeId) // Assuming this method exists in ChallengeDao
    }


    suspend fun updateChallengeProgressOnActivityLogged(userId: String, activityLog: ActivityLog) {
        val currentTime = System.currentTimeMillis()
        val activeUserChallenges = challengeDao.getActiveIncompleteUserChallenges(userId, currentTime)

        for (userChallenge in activeUserChallenges) {
            val challengeDetails = getChallengeDetails(userChallenge.challengeId) // Fetch base challenge details
            if (challengeDetails == null) {
                Log.w(TAG, "Challenge details not found for ID: ${userChallenge.challengeId}. Skipping progress update.")
                continue
            }

            // 1. Check if activity timestamp is within challenge duration
            if (activityLog.timestamp < userChallenge.startDate || activityLog.timestamp > userChallenge.endDate) {
                continue
            }

            // 2. Check activity type filter
            if (!challengeDetails.activityTypeFilter.isNullOrEmpty() &&
                !challengeDetails.activityTypeFilter.equals(activityLog.type, ignoreCase = true)) {
                continue
            }

            var progressIncrement = 0.0
            when (challengeDetails.challengeType) {
                ChallengeTypes.STEPS -> {
                    when (activityLog.type) {
                        AchievementChecker.RUNNING_GPS_TYPE -> {
                            val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                            progressIncrement = distanceKm * 1250 // Approx steps
                        }
                        "Walking", "Hiking" -> {
                            val distanceKm = calculateDistanceKmFromPath(activityLog.pathPoints)
                            if (distanceKm > 0) {
                                progressIncrement = distanceKm * 1500 // Approx steps
                            } else { // Fallback to duration based
                                progressIncrement = (activityLog.durationMillis / (1000.0 * 60.0)) * 100 // Approx steps/min
                            }
                        }
                        "Dancing" -> {
                             progressIncrement = (activityLog.durationMillis / (1000.0 * 60.0)) * 70 // Approx steps/min
                        }
                    }
                }
                ChallengeTypes.ACTIVE_MINUTES -> {
                    progressIncrement = activityLog.durationMillis / (1000.0 * 60.0) // Convert ms to minutes
                }
                ChallengeTypes.DISTANCE_KM -> {
                    if (activityLog.pathPoints.isNotEmpty()) {
                        progressIncrement = calculateDistanceKmFromPath(activityLog.pathPoints)
                    }
                }
                ChallengeTypes.LOG_ACTIVITY_COUNT -> {
                    progressIncrement = 1.0 // Each matching log counts as 1
                }
            }

            if (progressIncrement > 0) {
                val newProgress = userChallenge.currentProgress + progressIncrement
                val isCompleted = newProgress >= challengeDetails.targetValue

                try {
                    challengeDao.updateUserChallengeProgress(userChallenge.id, newProgress, isCompleted)
                    Log.d(TAG, "Challenge ${challengeDetails.name} progress updated to $newProgress for user $userId")

                    if (isCompleted && !userChallenge.isRewardClaimed) {
                        val userProfile = userProfileDao.getUserProfileSuspending(userId)
                        claimChallengeReward(userId, userChallenge.id, challengeDetails.xpReward, userProfile?.displayName, userProfile?.email)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating challenge progress for ${userChallenge.id}", e)
                }
            }
        }
    }

    suspend fun claimChallengeReward(userId: String, userChallengeId: Long, challengeXpReward: Int, userName: String?, email: String?): Result<String> {
        val userChallenge = challengeDao.getUserChallengeById(userChallengeId) // Need this method in DAO

        return if (userChallenge != null && userChallenge.userId == userId && userChallenge.isCompleted && !userChallenge.isRewardClaimed) {
            try {
                xpCalculator.addXP(userId, challengeXpReward, userName, email)
                challengeDao.claimReward(userChallengeId)
                val challengeDetails = getChallengeDetails(userChallenge.challengeId)
                val message = "Reward for '${challengeDetails?.name ?: "Challenge"}' claimed! +$challengeXpReward XP"
                Log.d(TAG, message)
                Result.Success(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error claiming reward for userChallengeId $userChallengeId", e)
                Result.Error(e)
            }
        } else {
            Result.Error(Exception("Reward already claimed, challenge not completed, or invalid user/challenge."))
        }
    }
    
    // Helper function from XPCalculator or common utility
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

// Need to add to ChallengeDao:
// @Query("SELECT * FROM challenges WHERE id = :challengeId")
// suspend fun getChallengeById(challengeId: String): Challenge?
//
// @Query("SELECT * FROM user_challenges WHERE id = :userChallengeId")
// suspend fun getUserChallengeById(userChallengeId: Long): UserChallenge?
