package com.example.fitnesstrackerapp.leaderboard.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.leaderboard.viewmodel.LeaderboardViewModel
import com.example.fitnesstrackerapp.leaderboard.viewmodel.LeaderboardViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var recyclerViewLeaderboard: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var progressBarLeaderboard: ProgressBar
    private lateinit var textViewNoLeaderboardData: TextView

    private val viewModel: LeaderboardViewModel by viewModels {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_LONG).show()
            finish()
            // Return a dummy factory or throw error, but finish should prevent ViewModel creation
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("User not logged in, cannot create LeaderboardViewModel")
                }
            }
        } else {
            LeaderboardViewModelFactory(application, currentUserUid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        supportActionBar?.title = "Leaderboard"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (FirebaseAuth.getInstance().currentUser?.uid == null) return // Safety check

        recyclerViewLeaderboard = findViewById(R.id.recyclerViewLeaderboard)
        progressBarLeaderboard = findViewById(R.id.progressBarLeaderboard)
        textViewNoLeaderboardData = findViewById(R.id.textViewNoLeaderboardData)

        setupRecyclerView()
        observeViewModel()
        
        // ViewModel calls fetchLeaderboard() in its init block.
        // If a manual refresh is desired, a SwipeRefreshLayout could be added,
        // or a refresh button, which would then call viewModel.fetchLeaderboard().
    }

    private fun setupRecyclerView() {
        leaderboardAdapter = LeaderboardAdapter()
        recyclerViewLeaderboard.apply {
            adapter = leaderboardAdapter
            layoutManager = LinearLayoutManager(this@LeaderboardActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBarLeaderboard.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.leaderboardList.observe(this) { leaderboardItems ->
            leaderboardAdapter.submitList(leaderboardItems)
            if (viewModel.isLoading.value == false && leaderboardItems.isNullOrEmpty()) {
                textViewNoLeaderboardData.visibility = View.VISIBLE
                recyclerViewLeaderboard.visibility = View.GONE
            } else {
                textViewNoLeaderboardData.visibility = View.GONE
                recyclerViewLeaderboard.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
