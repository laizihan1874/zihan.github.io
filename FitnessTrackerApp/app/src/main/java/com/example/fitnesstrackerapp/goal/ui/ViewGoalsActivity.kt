package com.example.fitnesstrackerapp.goal.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.goal.viewmodel.UserGoalsViewModel
import com.example.fitnesstrackerapp.goal.viewmodel.UserGoalsViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class ViewGoalsActivity : AppCompatActivity() {

    private lateinit var recyclerViewUserGoals: RecyclerView
    private lateinit var userGoalsAdapter: UserGoalsAdapter
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var textViewNoGoals: TextView

    private val viewModel: UserGoalsViewModel by viewModels {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_LONG).show()
            finish()
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("User not logged in for ViewGoalsActivity")
                }
            }
        } else {
            UserGoalsViewModelFactory(application, userId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_goals)

        supportActionBar?.title = "My Goals"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (FirebaseAuth.getInstance().currentUser?.uid == null) return

        recyclerViewUserGoals = findViewById(R.id.recyclerViewUserGoals)
        fabAddGoal = findViewById(R.id.fabAddGoal)
        textViewNoGoals = findViewById(R.id.textViewNoGoals)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userGoalsAdapter = UserGoalsAdapter { goalToDelete ->
            // Show confirmation dialog before deleting
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete this goal?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteGoal(goalToDelete.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        recyclerViewUserGoals.apply {
            adapter = userGoalsAdapter
            layoutManager = LinearLayoutManager(this@ViewGoalsActivity)
        }
    }

    private fun setupFab() {
        fabAddGoal.setOnClickListener {
            val intent = Intent(this, SetStructuredGoalActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.allUserGoals.observe(this) { goals ->
            userGoalsAdapter.submitList(goals)
            if (goals.isNullOrEmpty()) {
                textViewNoGoals.visibility = View.VISIBLE
                recyclerViewUserGoals.visibility = View.GONE
            } else {
                textViewNoGoals.visibility = View.GONE
                recyclerViewUserGoals.visibility = View.VISIBLE
            }
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
