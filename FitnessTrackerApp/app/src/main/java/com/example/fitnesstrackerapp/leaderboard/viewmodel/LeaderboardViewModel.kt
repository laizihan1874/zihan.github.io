package com.example.fitnesstrackerapp.leaderboard.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.fitnesstrackerapp.leaderboard.model.LeaderboardItem
import com.example.fitnesstrackerapp.social.repository.FriendRepository
import com.example.fitnesstrackerapp.tracking.model.AppDatabase // To get UserProfileDao
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class LeaderboardViewModel(
    application: Application,
    private val friendRepository: FriendRepository,
    private val userProfileDao: UserProfileDao,
    private val currentUserUid: String
) : AndroidViewModel(application) {

    private val _leaderboardList = MutableLiveData<List<LeaderboardItem>>()
    val leaderboardList: LiveData<List<LeaderboardItem>> = _leaderboardList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Fetch current user's profile
                val currentUserProfile = userProfileDao.getUserProfileSuspending(currentUserUid)

                // Fetch friends' profiles
                val friendsProfiles = friendRepository.getFriends(currentUserUid).first() // Collect first emission

                // Combine current user and friends
                val allProfilesForLeaderboard = mutableListOf<UserProfile>()
                currentUserProfile?.let { allProfilesForLeaderboard.add(it) }
                // Add friends, ensuring no duplicates if current user is somehow in friends list (shouldn't happen)
                friendsProfiles.forEach { friendProfile ->
                    if (allProfilesForLeaderboard.none { it.userId == friendProfile.userId }) {
                        allProfilesForLeaderboard.add(friendProfile)
                    }
                }
                
                // Sort by XP points descending
                val sortedProfiles = allProfilesForLeaderboard.sortedByDescending { it.xpPoints }

                // Transform to LeaderboardItem list
                val leaderboardItems = sortedProfiles.mapIndexed { index, profile ->
                    LeaderboardItem(
                        rank = index + 1,
                        userId = profile.userId,
                        displayName = profile.displayName ?: profile.email ?: "Unknown User",
                        xpPoints = profile.xpPoints,
                        isCurrentUser = profile.userId == currentUserUid
                    )
                }
                _leaderboardList.postValue(leaderboardItems)

            } catch (e: Exception) {
                // Handle error, e.g., post empty list or an error message
                _leaderboardList.postValue(emptyList())
                // Optionally, post an error event to be shown as a Toast
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

class LeaderboardViewModelFactory(
    private val application: Application,
    private val currentUserUid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
            val userProfileDao = AppDatabase.getDatabase(application).userProfileDao()
            // FriendRepository needs FirebaseFirestore instance. For simplicity, create it here.
            // In a larger app with DI, FriendRepository would be injected.
            val friendRepository = FriendRepository(FirebaseFirestore.getInstance())
            @Suppress("UNCHECKED_CAST")
            return LeaderboardViewModel(application, friendRepository, userProfileDao, currentUserUid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
