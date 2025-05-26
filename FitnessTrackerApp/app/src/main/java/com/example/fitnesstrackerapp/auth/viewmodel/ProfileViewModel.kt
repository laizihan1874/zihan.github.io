package com.example.fitnesstrackerapp.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.fitnesstrackerapp.gamification.logic.LevelingSystem
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope // For saveAssessment
import com.example.fitnesstrackerapp.utils.Event // For saveAssessment result
import kotlinx.coroutines.launch // For saveAssessment

class ProfileViewModel(application: Application, private val userProfileDao: UserProfileDao) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _userNameFromAuth = MutableLiveData<String?>() // For name from Firebase Auth or Intent
    val userNameFromAuth: LiveData<String?> = _userNameFromAuth

    // LiveData for UserProfile from Room, reacts to current user's UID
    val userProfile: LiveData<UserProfile?> = Transformations.switchMap(_currentUser) { firebaseUser ->
        firebaseUser?.uid?.let { userId ->
            userProfileDao.getUserProfile(userId)
        } ?: MutableLiveData<UserProfile?>(null) // Emit null if no user
    }

    val userLevelText: LiveData<String> = Transformations.map(userProfile) { profile ->
        profile?.let { "Level: ${it.level}" } ?: "Level: 1"
    }

    val userXPText: LiveData<String> = Transformations.map(userProfile) { profile ->
        profile?.let {
            val currentXP = it.xpPoints
            val xpForNextLevel = LevelingSystem.getXPForLevel(it.level + 1)
            if (xpForNextLevel == Long.MAX_VALUE) { // Max level reached
                "XP: $currentXP (Max Level)"
            } else {
                "XP: $currentXP / $xpForNextLevel"
            }
        } ?: "XP: 0 / ${LevelingSystem.getXPForLevel(2)}"
    }
    
    val xpProgressMax: LiveData<Int> = Transformations.map(userProfile) { profile ->
        profile?.let {
            val xpForCurrentLevelBase = LevelingSystem.getXPForLevel(it.level)
            val xpForNextLevelTarget = LevelingSystem.getXPForLevel(it.level + 1)
            if (xpForNextLevelTarget == Long.MAX_VALUE) { // Max level
                100 // Or currentXP - xpForCurrentLevelBase, with progress as 100
            } else {
                (xpForNextLevelTarget - xpForCurrentLevelBase).toInt()
            }
        } ?: (LevelingSystem.getXPForLevel(2) - LevelingSystem.getXPForLevel(1)).toInt()
    }

    val xpCurrentProgress: LiveData<Int> = Transformations.map(userProfile) { profile ->
        profile?.let {
            val xpForCurrentLevelBase = LevelingSystem.getXPForLevel(it.level)
            val xpForNextLevelTarget = LevelingSystem.getXPForLevel(it.level + 1)
             if (xpForNextLevelTarget == Long.MAX_VALUE) { // Max level
                100 // Fill the bar
            } else {
                (it.xpPoints - xpForCurrentLevelBase).toInt()
            }
        } ?: 0
    }

    private val _saveAssessmentResult = MutableLiveData<Event<Boolean>>()
    val saveAssessmentResult: LiveData<Event<Boolean>> = _saveAssessmentResult


    init {
        _currentUser.value = auth.currentUser
    }

    // Renamed from loadUser to avoid confusion with loading UserProfile from DB
    fun loadAuthUserDetails(nameFromIntent: String?) {
        _currentUser.value = auth.currentUser // Refresh current user state
        // Prefer name from intent if available (passed from registration), then Firebase, then null
        _userNameFromAuth.value = nameFromIntent ?: _currentUser.value?.displayName
    }

    fun saveAssessment(
        fitnessLevel: String?,
        primaryGoals: List<String>?,
        preferredActivities: List<String>?,
        age: Int?,
        gender: String?,
        weightKg: Float?,
        heightCm: Float?
    ) {
        val userId = _currentUser.value?.uid
        if (userId == null) {
            _saveAssessmentResult.value = Event(false) // No user, cannot save
            return
        }

        viewModelScope.launch {
            var currentProfile = userProfileDao.getUserProfileSuspending(userId)
            if (currentProfile == null) {
                // This case should ideally be covered by profile creation on login/registration
                // Creating a default one here if it somehow doesn't exist
                currentProfile = UserProfile(
                    userId = userId,
                    displayName = _userNameFromAuth.value ?: _currentUser.value?.displayName, // Use auth name
                    email = _currentUser.value?.email,
                    xpPoints = 0L,
                    level = 1
                )
            }

            val updatedProfile = currentProfile.copy(
                fitnessLevel = fitnessLevel,
                primaryGoals = primaryGoals,
                preferredActivities = preferredActivities,
                age = age,
                gender = gender,
                weightKg = weightKg,
                heightCm = heightCm
            )
            userProfileDao.upsertUserProfile(updatedProfile)
            _saveAssessmentResult.value = Event(true) // Indicate success
        }
    }


    fun signOut() {
        auth.signOut()
        _currentUser.value = null // Update LiveData after sign out
        // UserProfile LiveData will automatically update to null due to switchMap
    }
}
