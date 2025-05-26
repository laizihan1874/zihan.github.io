package com.example.fitnesstrackerapp.social.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.social.repository.FriendRepository
import com.example.fitnesstrackerapp.social.viewmodel.FriendsViewModel
import com.example.fitnesstrackerapp.social.viewmodel.FriendsViewModelFactory
import com.example.fitnesstrackerapp.utils.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsActivity : AppCompatActivity() {

    private lateinit var editTextSearchUsers: EditText
    private lateinit var buttonSearchUsers: Button
    private lateinit var recyclerViewUserSearchResults: RecyclerView
    private lateinit var recyclerViewFriendRequests: RecyclerView
    private lateinit var recyclerViewMyFriends: RecyclerView
    private lateinit var textViewNoFriendRequests: TextView
    private lateinit var textViewNoFriends: TextView

    private lateinit var userSearchAdapter: UserSearchAdapter
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private lateinit var friendListAdapter: FriendListAdapter

    private val viewModel: FriendsViewModel by viewModels {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_LONG).show()
            finish()
            // Return a dummy factory or throw error, but finish should prevent ViewModel creation
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("User not logged in, cannot create FriendsViewModel")
                }
            }
        } else {
            FriendsViewModelFactory(
                application,
                FriendRepository(FirebaseFirestore.getInstance()),
                currentUserUid
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        supportActionBar?.title = "Friends & Social"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (FirebaseAuth.getInstance().currentUser?.uid == null) {
            // This check is redundant if ViewModel init already finishes, but good for safety
            return
        }

        // Initialize UI elements
        editTextSearchUsers = findViewById(R.id.editTextSearchUsers)
        buttonSearchUsers = findViewById(R.id.buttonSearchUsers)
        recyclerViewUserSearchResults = findViewById(R.id.recyclerViewUserSearchResults)
        recyclerViewFriendRequests = findViewById(R.id.recyclerViewFriendRequests)
        recyclerViewMyFriends = findViewById(R.id.recyclerViewMyFriends)
        textViewNoFriendRequests = findViewById(R.id.textViewNoFriendRequests)
        textViewNoFriends = findViewById(R.id.textViewNoFriends)

        setupAdapters()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupAdapters() {
        userSearchAdapter = UserSearchAdapter { userToRequest ->
            viewModel.sendFriendRequest(userToRequest)
        }
        friendRequestAdapter = FriendRequestAdapter(
            onAcceptClicked = { requester -> viewModel.acceptFriendRequest(requester) },
            onDeclineClicked = { requester -> viewModel.declineFriendRequest(requester) }
        )
        friendListAdapter = FriendListAdapter { friendToRemove ->
            viewModel.removeFriend(friendToRemove)
        }
    }

    private fun setupRecyclerViews() {
        recyclerViewUserSearchResults.apply {
            adapter = userSearchAdapter
            layoutManager = LinearLayoutManager(this@FriendsActivity)
        }
        recyclerViewFriendRequests.apply {
            adapter = friendRequestAdapter
            layoutManager = LinearLayoutManager(this@FriendsActivity)
        }
        recyclerViewMyFriends.apply {
            adapter = friendListAdapter
            layoutManager = LinearLayoutManager(this@FriendsActivity)
        }
    }

    private fun setupClickListeners() {
        buttonSearchUsers.setOnClickListener {
            val query = editTextSearchUsers.text.toString().trim()
            viewModel.searchUsers(query)
        }
    }

    private fun observeViewModel() {
        viewModel.userSearchResults.observe(this) { users ->
            userSearchAdapter.submitList(users)
        }

        viewModel.friendRequests.observe(this) { requests ->
            friendRequestAdapter.submitList(requests)
            textViewNoFriendRequests.visibility = if (requests.isNullOrEmpty()) View.VISIBLE else View.GONE
            recyclerViewFriendRequests.visibility = if (requests.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.friendsList.observe(this) { friends ->
            friendListAdapter.submitList(friends)
            textViewNoFriends.visibility = if (friends.isNullOrEmpty()) View.VISIBLE else View.GONE
            recyclerViewMyFriends.visibility = if (friends.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.statusMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
