package com.example.fitnesstrackerapp.gamification.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.gamification.viewmodel.AchievementsViewModel
import com.example.fitnesstrackerapp.gamification.viewmodel.AchievementsViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class AchievementsActivity : AppCompatActivity() {

    private lateinit var recyclerViewAchievements: RecyclerView
    private lateinit var achievementsAdapter: AchievementsAdapter
    private lateinit var progressBarAchievements: ProgressBar
    private lateinit var textViewNoAchievements: TextView

    private val viewModel: AchievementsViewModel by viewModels {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // Handle user not logged in case, though ProfileActivity should prevent this
            // For now, can throw an exception or use a dummy ID if needed for ViewModel init,
            // but ideally this activity shouldn't be reached if userId is null.
            // Finish activity or show error message if no user.
            // For this example, let's assume ProfileActivity ensures user is logged in.
            // If not, the ViewModel will get an empty list for unlocked achievements.
            // A more robust solution might involve finishing if userId is null.
            // Let's proceed assuming userId is available.
            // If ProfileActivity guarantees user is logged in, this should be fine.
            AchievementsViewModelFactory(application, userId ?: "NO_USER_ID_ERROR")
        } else {
            AchievementsViewModelFactory(application, userId)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        supportActionBar?.title = "Achievements"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (FirebaseAuth.getInstance().currentUser?.uid == null) {
            // Optional: Show a message and finish if no user is logged in.
            // Toast.makeText(this, "User not logged in.", Toast.LENGTH_LONG).show()
            // finish()
            // return
        }

        recyclerViewAchievements = findViewById(R.id.recyclerViewAchievements)
        progressBarAchievements = findViewById(R.id.progressBarAchievements)
        textViewNoAchievements = findViewById(R.id.textViewNoAchievements)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        achievementsAdapter = AchievementsAdapter()
        recyclerViewAchievements.apply {
            adapter = achievementsAdapter
            layoutManager = LinearLayoutManager(this@AchievementsActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBarAchievements.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.achievementsToShow.observe(this) { achievementsList ->
            achievementsAdapter.submitList(achievementsList)
            if (viewModel.isLoading.value == false && achievementsList.isNullOrEmpty()) {
                textViewNoAchievements.visibility = View.VISIBLE
                recyclerViewAchievements.visibility = View.GONE
            } else {
                textViewNoAchievements.visibility = View.GONE
                recyclerViewAchievements.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
