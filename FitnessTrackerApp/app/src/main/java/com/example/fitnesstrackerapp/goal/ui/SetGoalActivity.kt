package com.example.fitnesstrackerapp.goal.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.goal.viewmodel.SetGoalViewModel

class SetGoalActivity : AppCompatActivity() {

    private val setGoalViewModel: SetGoalViewModel by viewModels()
    private lateinit var editTextGoalTargetCount: EditText
    private lateinit var buttonSaveGoal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_goal)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // For Up navigation
        supportActionBar?.title = "Set Weekly Goal"

        editTextGoalTargetCount = findViewById(R.id.editTextGoalTargetCount)
        buttonSaveGoal = findViewById(R.id.buttonSaveGoal)

        setGoalViewModel.currentGoalTarget.observe(this) { target ->
            target?.let {
                editTextGoalTargetCount.setText(it.toString())
            }
        }

        buttonSaveGoal.setOnClickListener {
            val targetStr = editTextGoalTargetCount.text.toString()
            if (targetStr.isEmpty()) {
                Toast.makeText(this, "Please enter a target number of workouts.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetCount = targetStr.toIntOrNull()
            if (targetCount == null || targetCount <= 0) {
                Toast.makeText(this, "Target must be a positive number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = setGoalViewModel.saveGoal(targetCount)
            if (success) {
                Toast.makeText(this, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // This case is currently handled by the validation above,
                // but good for future if saveGoal had more complex logic.
                Toast.makeText(this, "Failed to save goal. Invalid input.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
