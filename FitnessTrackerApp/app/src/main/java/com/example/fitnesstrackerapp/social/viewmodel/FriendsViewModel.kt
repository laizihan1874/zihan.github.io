package com.example.fitnesstrackerapp.social.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.fitnesstrackerapp.social.repository.FriendRepository
import com.example.fitnesstrackerapp.social.repository.Result // Import your Result class
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.utils.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class FriendsViewModel(
    application: Application,
    private val friendRepository: FriendRepository,
    private val currentUserUid: String
) : AndroidViewModel(application) {

    private val _userSearchResults = MutableLiveData<List<UserProfile>>()
    val userSearchResults: LiveData<List<UserProfile>> = _userSearchResults

    val friendRequests: LiveData<List<UserProfile>> =
        friendRepository.getFriendRequests(currentUserUid).asLiveData()

    val friendsList: LiveData<List<UserProfile>> =
        friendRepository.getFriends(currentUserUid).asLiveData()

    private val _statusMessage = MutableLiveData<Event<String>>()
    val statusMessage: LiveData<Event<String>> = _statusMessage

    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.length < 2) { // Basic validation
                _userSearchResults.value = emptyList()
                _statusMessage.value = Event("Search query must be at least 2 characters.")
                return@launch
            }
            val results = friendRepository.searchUsers(query, currentUserUid)
            _userSearchResults.value = results
            if (results.isEmpty()) {
                _statusMessage.value = Event("No users found for '$query'.")
            }
        }
    }

    fun sendFriendRequest(targetUser: UserProfile) {
        viewModelScope.launch {
            val result = friendRepository.sendFriendRequest(currentUserUid, targetUser.userId)
            when (result) {
                is Result.Success -> _statusMessage.value = Event("Friend request sent to ${targetUser.displayName ?: targetUser.email}.")
                is Result.Error -> _statusMessage.value = Event("Error sending request: ${result.exception.message}")
            }
        }
    }

    fun acceptFriendRequest(requester: UserProfile) {
        viewModelScope.launch {
            val result = friendRepository.acceptFriendRequest(currentUserUid, requester.userId)
            when (result) {
                is Result.Success -> _statusMessage.value = Event("${requester.displayName ?: requester.email} is now your friend!")
                is Result.Error -> _statusMessage.value = Event("Error accepting request: ${result.exception.message}")
            }
        }
    }

    fun declineFriendRequest(requester: UserProfile) {
        viewModelScope.launch {
            val result = friendRepository.declineOrRemoveFriend(currentUserUid, requester.userId)
            when (result) {
                is Result.Success -> _statusMessage.value = Event("Friend request from ${requester.displayName ?: requester.email} declined.")
                is Result.Error -> _statusMessage.value = Event("Error declining request: ${result.exception.message}")
            }
        }
    }

    fun removeFriend(friend: UserProfile) {
        viewModelScope.launch {
            val result = friendRepository.declineOrRemoveFriend(currentUserUid, friend.userId)
            when (result) {
                is Result.Success -> _statusMessage.value = Event("${friend.displayName ?: friend.email} removed from friends.")
                is Result.Error -> _statusMessage.value = Event("Error removing friend: ${result.exception.message}")
            }
        }
    }
}

class FriendsViewModelFactory(
    private val application: Application,
    private val friendRepository: FriendRepository,
    private val currentUserUid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsViewModel(application, friendRepository, currentUserUid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
