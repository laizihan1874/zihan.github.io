package com.example.fitnesstrackerapp.social.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.fitnesstrackerapp.social.repository.FriendRepository
import com.example.fitnesstrackerapp.social.repository.Result // Import your Result class
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FriendsViewModelTest {

    @get:Rule
    @JvmField
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApplication: Application // For AndroidViewModel

    @Mock
    private lateinit var mockFriendRepository: FriendRepository

    @Mock
    private lateinit var mockUserSearchResultsObserver: Observer<List<UserProfile>>
    @Mock
    private lateinit var mockFriendRequestsObserver: Observer<List<UserProfile>>
    @Mock
    private lateinit var mockFriendsListObserver: Observer<List<UserProfile>>
    @Mock
    private lateinit var mockStatusMessageObserver: Observer<Event<String>>

    private lateinit var viewModel: FriendsViewModel
    private val testCurrentUserUid = "testUserUid"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FriendsViewModel(mockApplication, mockFriendRepository, testCurrentUserUid)

        viewModel.userSearchResults.observeForever(mockUserSearchResultsObserver)
        viewModel.friendRequests.observeForever(mockFriendRequestsObserver)
        viewModel.friendsList.observeForever(mockFriendsListObserver)
        viewModel.statusMessage.observeForever(mockStatusMessageObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.userSearchResults.removeObserver(mockUserSearchResultsObserver)
        viewModel.friendRequests.removeObserver(mockFriendRequestsObserver)
        viewModel.friendsList.removeObserver(mockFriendsListObserver)
        viewModel.statusMessage.removeObserver(mockStatusMessageObserver)
    }

    @Test
    fun `searchUsers with valid query calls repository and updates LiveData`() = runTest(testDispatcher) {
        val query = "test"
        val mockResults = listOf(UserProfile("uid1", "Test User 1", "test1@example.com"))
        `when`(mockFriendRepository.searchUsers(query, testCurrentUserUid)).thenReturn(mockResults)

        viewModel.searchUsers(query)
        advanceUntilIdle() // Ensure coroutine completes

        verify(mockFriendRepository).searchUsers(query, testCurrentUserUid)
        verify(mockUserSearchResultsObserver).onChanged(mockResults)
    }

    @Test
    fun `searchUsers with short query posts status message and empty list`() = runTest(testDispatcher) {
        val query = "t"
        viewModel.searchUsers(query)
        advanceUntilIdle()

        verify(mockFriendRepository, never()).searchUsers(anyString(), anyString())
        verify(mockUserSearchResultsObserver).onChanged(emptyList())
        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("must be at least 2 characters") })
    }
    
    @Test
    fun `searchUsers with no results posts status message`() = runTest(testDispatcher) {
        val query = "nonexistent"
        `when`(mockFriendRepository.searchUsers(query, testCurrentUserUid)).thenReturn(emptyList())

        viewModel.searchUsers(query)
        advanceUntilIdle()
        
        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("No users found") })
    }


    @Test
    fun `sendFriendRequest success updates statusMessage`() = runTest(testDispatcher) {
        val targetUser = UserProfile("targetUid", "Target User", "target@example.com")
        `when`(mockFriendRepository.sendFriendRequest(testCurrentUserUid, targetUser.userId))
            .thenReturn(Result.Success(Unit))

        viewModel.sendFriendRequest(targetUser)
        advanceUntilIdle()

        verify(mockFriendRepository).sendFriendRequest(testCurrentUserUid, targetUser.userId)
        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("Friend request sent") })
    }

    @Test
    fun `sendFriendRequest error updates statusMessage`() = runTest(testDispatcher) {
        val targetUser = UserProfile("targetUid", "Target User", "target@example.com")
        val exception = Exception("Network error")
        `when`(mockFriendRepository.sendFriendRequest(testCurrentUserUid, targetUser.userId))
            .thenReturn(Result.Error(exception))

        viewModel.sendFriendRequest(targetUser)
        advanceUntilIdle()

        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("Error sending request: Network error") })
    }

    @Test
    fun `acceptFriendRequest success updates statusMessage`() = runTest(testDispatcher) {
        val requester = UserProfile("requesterUid", "Requester User", "req@example.com")
        `when`(mockFriendRepository.acceptFriendRequest(testCurrentUserUid, requester.userId))
            .thenReturn(Result.Success(Unit))

        viewModel.acceptFriendRequest(requester)
        advanceUntilIdle()

        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("is now your friend") })
    }
    
    @Test
    fun `declineFriendRequest success updates statusMessage`() = runTest(testDispatcher) {
        val requester = UserProfile("requesterUid", "Requester User", "req@example.com")
        `when`(mockFriendRepository.declineOrRemoveFriend(testCurrentUserUid, requester.userId))
            .thenReturn(Result.Success(Unit))

        viewModel.declineFriendRequest(requester)
        advanceUntilIdle()

        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("request from ${requester.displayName} declined") })
    }

    @Test
    fun `removeFriend success updates statusMessage`() = runTest(testDispatcher) {
        val friend = UserProfile("friendUid", "Friend User", "friend@example.com")
        `when`(mockFriendRepository.declineOrRemoveFriend(testCurrentUserUid, friend.userId))
            .thenReturn(Result.Success(Unit))

        viewModel.removeFriend(friend)
        advanceUntilIdle()

        verify(mockStatusMessageObserver).onChanged(argThat { it.peekContent().contains("${friend.displayName} removed from friends") })
    }


    @Test
    fun `friendRequests LiveData emits data from repository Flow`() = runTest(testDispatcher) {
        val mockRequests = listOf(UserProfile("req1", "Req 1", "r1@example.com"))
        `when`(mockFriendRepository.getFriendRequests(testCurrentUserUid)).thenReturn(flowOf(mockRequests))
        
        // Re-initialize ViewModel to re-trigger asLiveData on the flow or use a trigger
        // For this test, the flow is converted to LiveData in init.
        // We need to ensure the observer gets the value from the flow.
        // The current setup should work if the flow emits upon collection (asLiveData does this).
        val newViewModel = FriendsViewModel(mockApplication, mockFriendRepository, testCurrentUserUid)
        newViewModel.friendRequests.observeForever(mockFriendRequestsObserver)
        advanceUntilIdle() // Allow flow collection and LiveData update

        verify(mockFriendRequestsObserver).onChanged(mockRequests)
        newViewModel.friendRequests.removeObserver(mockFriendRequestsObserver) // Clean up
    }

    @Test
    fun `friendsList LiveData emits data from repository Flow`() = runTest(testDispatcher) {
        val mockFriends = listOf(UserProfile("friend1", "Friend 1", "f1@example.com"))
        `when`(mockFriendRepository.getFriends(testCurrentUserUid)).thenReturn(flowOf(mockFriends))

        val newViewModel = FriendsViewModel(mockApplication, mockFriendRepository, testCurrentUserUid)
        newViewModel.friendsList.observeForever(mockFriendsListObserver)
        advanceUntilIdle()

        verify(mockFriendsListObserver).onChanged(mockFriends)
        newViewModel.friendsList.removeObserver(mockFriendsListObserver) // Clean up
    }
}
