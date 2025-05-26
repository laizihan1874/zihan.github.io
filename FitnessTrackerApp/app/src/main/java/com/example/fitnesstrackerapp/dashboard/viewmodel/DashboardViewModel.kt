package com.example.fitnesstrackerapp.dashboard.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.example.fitnesstrackerapp.goal.viewmodel.SetGoalViewModel // For PREFS_NAME and KEYS
import com.example.fitnesstrackerapp.tracking.model.ActivityDao
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.example.fitnesstrackerapp.goal.model.UserGoalDao // For SuggestionEngine
import com.example.fitnesstrackerapp.user.model.UserProfileDao // For SuggestionEngine
import com.example.fitnesstrackerapp.personalization.logic.SuggestionEngine // Added
import kotlinx.coroutines.launch // Added for viewModelScope
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao: ActivityDao
    private val userProfileDao: UserProfileDao // Added for SuggestionEngine
    private val userGoalDao: UserGoalDao     // Added for SuggestionEngine
    private val suggestionEngine: SuggestionEngine // Added

    val allActivities: LiveData<List<ActivityLog>>

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences(SetGoalViewModel.PREFS_NAME, Context.MODE_PRIVATE)

    private val _goalTargetCount = MutableLiveData<Int?>()
    val goalTargetCount: LiveData<Int?> = _goalTargetCount

    private val _goalProgressCount = MediatorLiveData<Int>()
    val goalProgressCount: LiveData<Int> = _goalProgressCount

    private val _goalIsSet = MutableLiveData<Boolean>()
    val goalIsSet: LiveData<Boolean> = _goalIsSet

    // To trigger recalculation when goal start date changes
    private val _goalStartTimestamp = MutableLiveData<Long?>()

    // For workout suggestion
    private val _workoutSuggestion = MutableLiveData<String?>()
    val workoutSuggestion: LiveData<String?> = _workoutSuggestion

    init {
        val database = AppDatabase.getDatabase(application)
        activityDao = database.activityDao()
        userProfileDao = database.userProfileDao() // Initialize
        userGoalDao = database.userGoalDao()       // Initialize
        suggestionEngine = SuggestionEngine(userProfileDao, activityDao, userGoalDao) // Initialize

        allActivities = activityDao.getAllActivityLogs() // This likely needs to be user-specific if not already

        loadGoalData() // Load initial goal data

        _goalProgressCount.addSource(allActivities) { activities ->
            calculateProgress(activities, _goalStartTimestamp.value, _goalTargetCount.value)
        }
        _goalProgressCount.addSource(_goalStartTimestamp) { timestamp -> 
            calculateProgress(allActivities.value, timestamp, _goalTargetCount.value)
        }
         _goalProgressCount.addSource(_goalTargetCount) { target -> 
            calculateProgress(allActivities.value, _goalStartTimestamp.value, target)
        }
        // Note: `allActivities` should ideally be user-specific.
        // If `ActivityDao.getAllActivityLogs()` isn't user-specific, this will show all logs.
        // Assuming ActivityLog has a userId field and DAO method is updated or a new user-specific one is used.
        // For now, proceeding with existing structure.
    }

    fun loadGoalData() {
        val isSet = sharedPreferences.contains(SetGoalViewModel.KEY_GOAL_TARGET_COUNT) &&
                    sharedPreferences.contains(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP)
        _goalIsSet.value = isSet

        if (isSet) {
            _goalTargetCount.value = sharedPreferences.getInt(SetGoalViewModel.KEY_GOAL_TARGET_COUNT, 0)
            _goalStartTimestamp.value = sharedPreferences.getLong(SetGoalViewModel.KEY_GOAL_START_TIMESTAMP, 0L)
        } else {
            _goalTargetCount.value = null
            _goalStartTimestamp.value = null
            _goalProgressCount.value = 0 // Explicitly set progress to 0 if no goal
        }
        // Trigger a recalculation if allActivities already has a value
        if(allActivities.value != null) {
            calculateProgress(allActivities.value, _goalStartTimestamp.value, _goalTargetCount.value)
        }
    }

    private fun calculateProgress(activities: List<ActivityLog>?, goalStartTime: Long?, target: Int?) {
        if (activities == null || goalStartTime == null || target == null || target == 0 || _goalIsSet.value == false) {
            _goalProgressCount.value = 0
            return
        }

        // Simple "this week" logic: activities logged from goalStartTime up to 7 days later.
        // More complex: align to calendar week (Mon-Sun) that contains goalStartTime.
        // For now, using the simple 7-day window.
        val weekEndTime = goalStartTime + (7 * 24 * 60 * 60 * 1000)

        val relevantActivities = activities.filter {
            it.timestamp >= goalStartTime && it.timestamp < weekEndTime
        }
        _goalProgressCount.value = relevantActivities.size
    }

    // Consider adding a SharedPreferences.OnSharedPreferenceChangeListener
    // to automatically call loadGoalData() if goal prefs change while app is running.
    // This is more robust but adds complexity. For now, call loadGoalData() from MainActivity's onResume.

    fun fetchWorkoutSuggestion(userId: String) {
        viewModelScope.launch {
            _workoutSuggestion.postValue(suggestionEngine.getWorkoutSuggestion(userId))
        }
    }
}
