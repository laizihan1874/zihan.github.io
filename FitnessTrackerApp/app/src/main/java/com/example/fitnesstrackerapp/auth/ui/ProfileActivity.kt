package com.example.fitnesstrackerapp.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.auth.viewmodel.ProfileViewModel
import com.example.fitnesstrackerapp.auth.viewmodel.ProfileViewModelFactory
import com.example.fitnesstrackerapp.gamification.ui.AchievementsActivity
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.google.firebase.auth.FirebaseUser
import android.widget.ProgressBar

class ProfileActivity : AppCompatActivity() {

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(application, AppDatabase.getDatabase(applicationContext).userProfileDao())
    }

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var textViewUserLevel: TextView
    private lateinit var textViewUserXP: TextView
    private lateinit var progressBarXP: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Ensure Up navigation is enabled
        supportActionBar?.title = "Profile"

        userNameTextView = findViewById(R.id.textViewUserName)
        userEmailTextView = findViewById(R.id.textViewUserEmail)
        textViewUserLevel = findViewById(R.id.textViewUserLevel)
        textViewUserXP = findViewById(R.id.textViewUserXP)
        progressBarXP = findViewById(R.id.progressBarXP)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)
        val viewAchievementsButton = findViewById<Button>(R.id.buttonViewAchievements)
        val friendsButton = findViewById<Button>(R.id.buttonFriends)
        val fitnessAssessmentButton = findViewById<Button>(R.id.buttonFitnessAssessment)
        val myGoalsButton = findViewById<Button>(R.id.buttonMyGoals)
        val leaderboardButton = findViewById<Button>(R.id.buttonLeaderboard) // Added

        val nameFromIntent = intent.getStringExtra("USER_NAME")
        profileViewModel.loadAuthUserDetails(nameFromIntent) // Call this to load name from intent/Firebase

        // Observe FirebaseUser for basic auth info (email) and logout navigation
        profileViewModel.currentUser.observe(this) { user ->
            if (user == null) {
                navigateToLogin()
            } else {
                userEmailTextView.text = "Email: ${user.email ?: "N/A"}"
            }
        }
        // Observe name from Auth/Intent
        profileViewModel.userNameFromAuth.observe(this) { name ->
             userNameTextView.text = "Name: ${name ?: "N/A"}" // Fallback to N/A if null
        }

        // Observe UserProfile LiveData from ViewModel for Level and XP
        profileViewModel.userLevelText.observe(this) { levelText ->
            textViewUserLevel.text = levelText
        }
        profileViewModel.userXPText.observe(this) { xpText ->
            textViewUserXP.text = xpText
        }
        profileViewModel.xpProgressMax.observe(this) { maxProgress ->
            progressBarXP.max = maxProgress
        }
        profileViewModel.xpCurrentProgress.observe(this) { currentProgress ->
            progressBarXP.progress = currentProgress
        }

        logoutButton.setOnClickListener {
            profileViewModel.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            // Observer on currentUser will handle navigation to login
        }

        viewAchievementsButton.setOnClickListener {
            val intent = Intent(this, AchievementsActivity::class.java)
            startActivity(intent)
        }

        friendsButton.setOnClickListener { // Added
            val intent = Intent(this, com.example.fitnesstrackerapp.social.ui.FriendsActivity::class.java)
            startActivity(intent)
        }

        fitnessAssessmentButton.setOnClickListener { // Added
            val intent = Intent(this, com.example.fitnesstrackerapp.user.ui.FitnessAssessmentActivity::class.java)
            // No need to pass extras, FitnessAssessmentActivity will load UserProfile via ViewModel
            startActivity(intent)
        }

        myGoalsButton.setOnClickListener { // Added
            val intent = Intent(this, com.example.fitnesstrackerapp.goal.ui.ViewGoalsActivity::class.java)
            startActivity(intent)
        }

        leaderboardButton.setOnClickListener { // Added
            val intent = Intent(this, com.example.fitnesstrackerapp.leaderboard.ui.LeaderboardActivity::class.java)
            startActivity(intent)
        }
    }

    // The old updateUI method is no longer needed as LiveData observers handle UI updates.

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
