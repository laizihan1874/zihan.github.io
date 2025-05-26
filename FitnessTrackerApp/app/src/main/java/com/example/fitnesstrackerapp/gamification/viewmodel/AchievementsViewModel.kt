package com.example.fitnesstrackerapp.gamification.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.gamification.model.AchievementDao
import com.example.fitnesstrackerapp.gamification.model.UserAchievement
import com.example.fitnesstrackerapp.gamification.ui.AchievementDisplayItem
import com.example.fitnesstrackerapp.tracking.model.AppDatabase

class AchievementsViewModel(application: Application, private val userId: String) : AndroidViewModel(application) {

    private val achievementDao: AchievementDao
    private val _achievementsToShow = MediatorLiveData<List<AchievementDisplayItem>>()
    val achievementsToShow: LiveData<List<AchievementDisplayItem>> = _achievementsToShow

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var allAchievementsSource: LiveData<List<Achievement>>
    private var unlockedAchievementsSource: LiveData<List<UserAchievement>>

    init {
        achievementDao = AppDatabase.getDatabase(application).achievementDao()
        allAchievementsSource = achievementDao.getAllAchievements()
        unlockedAchievementsSource = achievementDao.getUnlockedAchievementsForUser(userId)

        _isLoading.value = true

        _achievementsToShow.addSource(allAchievementsSource) { all ->
            combineAchievements(all, unlockedAchievementsSource.value)
        }

        _achievementsToShow.addSource(unlockedAchievementsSource) { unlocked ->
            combineAchievements(allAchievementsSource.value, unlocked)
        }
    }

    private fun combineAchievements(
        all: List<Achievement>?,
        unlocked: List<UserAchievement>?
    ) {
        if (all == null) { // Wait for all achievements to load at least
            _isLoading.value = true // Still loading if 'all' isn't there yet
            return
        }
        _isLoading.value = false // Data is ready or will be empty list

        val unlockedMap = unlocked?.associateBy { it.achievementId } ?: emptyMap()

        val displayList = all.map { achievement ->
            AchievementDisplayItem(achievement, unlockedMap[achievement.id])
        }
        _achievementsToShow.value = displayList
    }
}

class AchievementsViewModelFactory(
    private val application: Application,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AchievementsViewModel(application, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
