package com.example.fitnesstrackerapp.goal.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.fitnesstrackerapp.goal.model.UserGoal
import com.example.fitnesstrackerapp.goal.model.UserGoalDao
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.example.fitnesstrackerapp.utils.Event
import kotlinx.coroutines.launch

class UserGoalsViewModel(
    application: Application,
    private val userGoalDao: UserGoalDao,
    private val userId: String
) : AndroidViewModel(application) {

    val allUserGoals: LiveData<List<UserGoal>> = userGoalDao.getAllUserGoals(userId)

    private val _statusMessage = MutableLiveData<Event<String>>()
    val statusMessage: LiveData<Event<String>> = _statusMessage

    fun saveGoal(goal: UserGoal) {
        viewModelScope.launch {
            try {
                userGoalDao.upsertUserGoal(goal)
                _statusMessage.value = Event("Goal saved successfully!")
            } catch (e: Exception) {
                _statusMessage.value = Event("Error saving goal: ${e.message}")
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                userGoalDao.deleteGoal(goalId)
                _statusMessage.value = Event("Goal deleted.")
            } catch (e: Exception) {
                _statusMessage.value = Event("Error deleting goal: ${e.message}")
            }
        }
    }
    
    // Placeholder for fetching current weight if needed for WEIGHT_TARGET currentValue
    // suspend fun getCurrentWeight(userId: String): Float? {
    //     // val userProfileDao = AppDatabase.getDatabase(getApplication()).userProfileDao()
    //     // return userProfileDao.getUserProfileSuspending(userId)?.weightKg
    //     return null // Implement if UserProfile and its DAO are accessible here
    // }
}

class UserGoalsViewModelFactory(
    private val application: Application,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserGoalsViewModel::class.java)) {
            val userGoalDao = AppDatabase.getDatabase(application).userGoalDao()
            @Suppress("UNCHECKED_CAST")
            return UserGoalsViewModel(application, userGoalDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
