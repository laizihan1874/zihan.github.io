package com.example.fitnesstrackerapp.tracking.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fitnesstrackerapp.tracking.model.ActivityDao
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.example.fitnesstrackerapp.gamification.logic.LevelingSystem // For XPCalculator
import com.example.fitnesstrackerapp.gamification.logic.XPCalculator
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.fitnesstrackerapp.goal.model.GoalType // Added
import com.example.fitnesstrackerapp.goal.model.UserGoalDao 
import com.example.fitnesstrackerapp.gamification.logic.AchievementChecker 
import com.example.fitnesstrackerapp.challenge.logic.ChallengeService // Added
import com.example.fitnesstrackerapp.challenge.model.ChallengeDao // Added

class LogActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao: ActivityDao
    private val userProfileDao: UserProfileDao
    private val xpCalculator: XPCalculator
    private val userGoalDao: UserGoalDao
    private val challengeDao: ChallengeDao // Added
    private val challengeService: ChallengeService // Added


    // Existing LiveData for manual save result
    private val _saveResult = MutableLiveData<Pair<Boolean, String?>>() 
    val saveResult: LiveData<Pair<Boolean, String?>> = _saveResult

    init {
        val database = AppDatabase.getDatabase(application)
        activityDao = database.activityDao()
        userProfileDao = database.userProfileDao()
        userGoalDao = database.userGoalDao()
        challengeDao = database.challengeDao() // Added
        xpCalculator = XPCalculator(userProfileDao, LevelingSystem)
        challengeService = ChallengeService(challengeDao, userProfileDao, xpCalculator, application) // Added
    }

    fun saveActivity(
        type: String,
        timestamp: Long, // Combined date and time in millis
        durationMinutes: Long,
        calories: Int,
        notes: String?
    ) {
        if (type.isEmpty()) {
            _saveResult.value = Pair(false, "Activity type cannot be empty.")
            return
        }
        if (timestamp <= 0) {
            _saveResult.value = Pair(false, "Please select a valid date and time.")
            return
        }
        if (durationMinutes <= 0) {
            _saveResult.value = Pair(false, "Duration must be greater than zero.")
            return
        }
        if (calories <= 0) {
            _saveResult.value = Pair(false, "Calories burned must be greater than zero.")
            return
        }

        val durationMillis = durationMinutes * 60 * 1000 // Convert minutes to milliseconds

        val newLog = ActivityLog(
            type = type,
            timestamp = timestamp,
            durationMillis = durationMillis,
            caloriesBurned = calories,
            notes = notes
        )

        viewModelScope.launch {
            try {
                activityDao.insertActivityLog(newLog)
                // Award XP
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    xpCalculator.addXPForActivity(
                        currentUser.uid,
                        newLog, // Pass the just saved log
                        currentUser.displayName,
                        currentUser.email
                    )
                    updateGoalProgressAfterActivity(currentUser.uid, newLog)
                    challengeService.updateChallengeProgressOnActivityLogged(currentUser.uid, newLog) // Added
                }
                _saveResult.postValue(Pair(true, "Manual activity saved successfully!"))
            } catch (e: Exception) {
                _saveResult.postValue(Pair(false, "Error saving manual activity: ${e.message}"))
            }
        }
    }

    fun saveTrackedActivity(
        activityType: String,
        startTimeMillis: Long,
        durationMillis: Long,
        totalDistanceMeters: Float, 
        pathPointsStrings: List<String>,
        estimatedCalories: Int?
    ) {
        if (activityType.isEmpty() || startTimeMillis <= 0 || durationMillis <= 0) {
            _saveResult.value = Pair(false, "Invalid activity data for tracked activity.")
            return
        }

        val caloriesToSave = estimatedCalories ?: 0

        val newActivityLog = ActivityLog(
            type = activityType,
            timestamp = startTimeMillis,
            durationMillis = durationMillis,
            caloriesBurned = caloriesToSave,
            notes = "GPS Tracked", 
            pathPoints = pathPointsStrings
        )

        viewModelScope.launch {
            try {
                activityDao.insertActivityLog(newActivityLog)
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    xpCalculator.addXPForActivity(
                        currentUser.uid,
                        newActivityLog, 
                        currentUser.displayName,
                        currentUser.email
                    )
                    updateGoalProgressAfterActivity(currentUser.uid, newActivityLog)
                    challengeService.updateChallengeProgressOnActivityLogged(currentUser.uid, newActivityLog) // Added
                }
                _saveResult.postValue(Pair(true, "Tracked activity saved successfully!"))
            } catch (e: Exception) {
                _saveResult.postValue(Pair(false, "Error saving tracked activity: ${e.message}"))
            }
        }
    }

    private suspend fun updateGoalProgressAfterActivity(userId: String, savedActivityLog: ActivityLog) {
        val activeGoals = userGoalDao.getActiveUserGoals(userId)

        for (goal in activeGoals) {
            // Check activity type filter
            if (!goal.activityTypeFilter.isNullOrEmpty() && goal.activityTypeFilter != savedActivityLog.type) {
                continue // Skip this goal if activity type doesn't match filter
            }

            var valueToAdd = 0.0
            val goalTypeEnum = try { GoalType.valueOf(goal.goalType) } catch (e: IllegalArgumentException) { null }

            when (goalTypeEnum) {
                GoalType.WEEKLY_DISTANCE_RUN -> {
                    if (savedActivityLog.type == AchievementChecker.RUNNING_GPS_TYPE || goal.activityTypeFilter == savedActivityLog.type) {
                        // Calculate distance from pathPoints for accuracy
                        var distanceMeters = 0f
                        val latLngList = savedActivityLog.pathPoints.mapNotNull {
                            val parts = it.split(",")
                            if (parts.size == 2) Pair(parts[0].toDouble(), parts[1].toDouble()) else null
                        }
                        for (i in 0 until latLngList.size - 1) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                latLngList[i].first, latLngList[i].second,
                                latLngList[i+1].first, latLngList[i+1].second,
                                results
                            )
                            distanceMeters += results[0]
                        }
                        if (goalTypeEnum.unit == "km") {
                            valueToAdd = distanceMeters / 1000.0
                        } else { // Assuming meters if not km, though goal type specifies unit
                            valueToAdd = distanceMeters.toDouble()
                        }
                    }
                }
                GoalType.WEEKLY_DURATION_CYCLE -> {
                     if (savedActivityLog.type == "Cycling (GPS)" || goal.activityTypeFilter == savedActivityLog.type) { // Assuming "Cycling (GPS)" type
                        if (goalTypeEnum.unit == "hours") {
                            valueToAdd = savedActivityLog.durationMillis / (1000.0 * 60.0 * 60.0)
                        } else { // Assuming minutes if not hours
                            valueToAdd = savedActivityLog.durationMillis / (1000.0 * 60.0)
                        }
                    }
                }
                GoalType.DAILY_STEP_COUNT -> {
                    when (savedActivityLog.type) {
                        AchievementChecker.RUNNING_GPS_TYPE -> {
                            var distanceMeters = 0f
                             savedActivityLog.pathPoints.mapNotNull {
                                val parts = it.split(",")
                                if (parts.size == 2) Pair(parts[0].toDouble(), parts[1].toDouble()) else null
                            }.let { latLngList ->
                                for (i in 0 until latLngList.size - 1) {
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(latLngList[i].first, latLngList[i].second, latLngList[i+1].first, latLngList[i+1].second, results)
                                    distanceMeters += results[0]
                                }
                            }
                            valueToAdd = (distanceMeters / 1000.0) * 1250 // Approx 1250 steps/km for running
                        }
                        "Walking", "Hiking" -> {
                            var distanceMeters = 0f
                            val latLngList = savedActivityLog.pathPoints.mapNotNull {
                                val parts = it.split(",")
                                if (parts.size == 2) Pair(parts[0].toDouble(), parts[1].toDouble()) else null
                            }
                            if (latLngList.size > 1) { // Check if there are enough points to calculate distance
                                for (i in 0 until latLngList.size - 1) {
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        latLngList[i].first, latLngList[i].second,
                                        latLngList[i+1].first, latLngList[i+1].second,
                                        results
                                    )
                                    distanceMeters += results[0]
                                }
                                valueToAdd = (distanceMeters / 1000.0) * 1500 // Approx 1500 steps/km for walking/hiking
                            } else { // Estimate from duration if no path
                                valueToAdd = (savedActivityLog.durationMillis / (1000.0 * 60.0)) * 100 // Approx 100 steps/min
                            }
                        }
                        "Dancing" -> { // Rough estimate for dancing
                            valueToAdd = (savedActivityLog.durationMillis / (1000.0 * 60.0)) * 70 // Approx 70 steps/min for dancing
                        }
                        // Other types like Swimming, Yoga, Pilates, Weight Training, Martial Arts, Team Sport
                        // generally don't contribute directly to step count goals in this manner.
                    }
                }
                GoalType.WEIGHT_TARGET -> { /* Skip, updated manually or via a dedicated UI */ }
                null -> { /* Unknown goal type string in DB or unhandled GoalType */ }
            }

            if (valueToAdd > 0) {
                val newProgress = goal.currentValue + valueToAdd
                val isCompleted = newProgress >= goal.targetValue
                try {
                    userGoalDao.updateGoalProgress(goal.id, newProgress, System.currentTimeMillis(), isCompleted)
                    // TODO: Maybe post an event if a goal is completed for UI feedback (e.g., Toast)
                } catch (e: Exception) {
                    // Log error updating goal progress
                }
            }
        }
    }
}
