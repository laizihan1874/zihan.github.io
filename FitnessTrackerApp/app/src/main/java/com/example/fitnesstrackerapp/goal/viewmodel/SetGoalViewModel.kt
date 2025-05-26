package com.example.fitnesstrackerapp.goal.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SetGoalViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PREFS_NAME = "FitnessGoalPrefs"
        const val KEY_GOAL_TYPE = "goal_type"
        const val KEY_GOAL_TARGET_COUNT = "goal_target_count"
        const val KEY_GOAL_START_TIMESTAMP = "goal_start_timestamp"
        const val DEFAULT_GOAL_TYPE = "log_workouts_this_week"
    }

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentGoalTarget = MutableLiveData<Int?>()
    val currentGoalTarget: LiveData<Int?> = _currentGoalTarget

    init {
        loadGoalTargetForEditing()
    }

    private fun loadGoalTargetForEditing() {
        if (sharedPreferences.contains(KEY_GOAL_TARGET_COUNT)) {
            _currentGoalTarget.value = sharedPreferences.getInt(KEY_GOAL_TARGET_COUNT, 0)
        } else {
            _currentGoalTarget.value = null
        }
    }

    fun saveGoal(targetCount: Int): Boolean {
        if (targetCount <= 0) {
            return false // Indicate failure due to invalid input
        }
        with(sharedPreferences.edit()) {
            putString(KEY_GOAL_TYPE, DEFAULT_GOAL_TYPE)
            putInt(KEY_GOAL_TARGET_COUNT, targetCount)
            putLong(KEY_GOAL_START_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        return true
    }

    // This function might not be strictly needed if SetGoalActivity always fetches for pre-filling
    // but can be useful if the ViewModel is shared or for other UI logic.
    fun getGoalTarget(): Int? {
        return if (sharedPreferences.contains(KEY_GOAL_TARGET_COUNT)) {
            sharedPreferences.getInt(KEY_GOAL_TARGET_COUNT, 0)
        } else {
            null
        }
    }
     fun getGoalStartTimestamp(): Long {
        return sharedPreferences.getLong(KEY_GOAL_START_TIMESTAMP, 0L)
    }

    fun getGoalType(): String? {
        return sharedPreferences.getString(KEY_GOAL_TYPE, null)
    }

    fun isGoalSet(): Boolean {
        return sharedPreferences.contains(KEY_GOAL_TARGET_COUNT) &&
               sharedPreferences.contains(KEY_GOAL_START_TIMESTAMP)
    }
}
