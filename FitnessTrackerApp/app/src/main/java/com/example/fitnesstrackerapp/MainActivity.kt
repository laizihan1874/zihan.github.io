package com.example.fitnesstrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.auth.ui.ProfileActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.auth.ui.ProfileActivity
import com.example.fitnesstrackerapp.dashboard.ui.ActivityListAdapter
import com.example.fitnesstrackerapp.dashboard.viewmodel.DashboardViewModel
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.auth.ui.ProfileActivity
import com.example.fitnesstrackerapp.dashboard.ui.ActivityListAdapter
import com.example.fitnesstrackerapp.dashboard.viewmodel.DashboardViewModel
import com.example.fitnesstrackerapp.goal.ui.SetGoalActivity
import com.example.fitnesstrackerapp.tracking.ui.LogActivityActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit
import android.app.Activity
import android.util.Log


class MainActivity : AppCompatActivity() {

    private var userName: String? = null
    private var userEmail: String? = null

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var activityListAdapter: ActivityListAdapter
    private lateinit var recyclerViewActivities: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var textViewGoalDescription: TextView
    private lateinit var textViewGoalProgress: TextView
    private lateinit var buttonSetGoal: Button

    // Workout Suggestion
    private lateinit var cardViewWorkoutSuggestion: androidx.cardview.widget.CardView
    private lateinit var textViewWorkoutSuggestion: TextView

    // Google Fit
    private lateinit var buttonConnectGoogleFit: Button
    private lateinit var textViewGoogleFitSteps: TextView
    private val REQUEST_OAUTH_REQUEST_CODE = 1002
    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "Dashboard"

        userName = intent.getStringExtra("USER_NAME")
        userEmail = intent.getStringExtra("USER_EMAIL")

        setupRecyclerView()
        setupBottomNavigation()
        setupGoalUI()
        setupGoogleFitUI()
        setupWorkoutSuggestionUI() // New UI setup

        observeViewModel()
        checkAndReadFitDataOnStart()
    }

    private fun setupWorkoutSuggestionUI() {
        cardViewWorkoutSuggestion = findViewById(R.id.cardViewWorkoutSuggestion)
        textViewWorkoutSuggestion = findViewById(R.id.textViewWorkoutSuggestion)

        // Premium Coaching Stub Button
        val buttonLearnMoreCoaching: Button = findViewById(R.id.buttonLearnMoreCoaching)
        buttonLearnMoreCoaching.setOnClickListener {
            val intent = Intent(this, com.example.fitnesstrackerapp.premium.ui.PremiumFeaturesPlaceholderActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupGoogleFitUI() {
        buttonConnectGoogleFit = findViewById(R.id.buttonConnectGoogleFit)
        textViewGoogleFitSteps = findViewById(R.id.textViewGoogleFitSteps)
        buttonConnectGoogleFit.setOnClickListener {
            requestFitPermissions()
        }
    }

    private fun checkAndReadFitDataOnStart() {
        val googleSignInAccount = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        if (GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
            readDailyStepCount(googleSignInAccount)
            updateUIVisibility(isConnected = true)
        } else {
            updateUIVisibility(isConnected = false)
        }
    }

    private fun requestFitPermissions() {
        val googleSignInAccount = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        GoogleSignIn.requestPermissions(
            this,
            REQUEST_OAUTH_REQUEST_CODE,
            googleSignInAccount,
            fitnessOptions
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i("MainActivityFit", "Google Fit Permission Granted")
                readDailyStepCount(GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                updateUIVisibility(isConnected = true)
            } else {
                Log.w("MainActivityFit", "Google Fit Permission Denied")
                Toast.makeText(this, "Permission denied to Google Fit.", Toast.LENGTH_SHORT).show()
                updateUIVisibility(isConnected = false)
                updateStepsUI(error = "Permission denied.")
            }
        }
    }

    private fun readDailyStepCount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val totalSteps = response.buckets
                    .flatMap { it.dataSets }
                    .flatMap { it.dataPoints }
                    .sumOf { dataPoint ->
                        dataPoint.getValue(Field.FIELD_STEPS).asInt()
                    }
                Log.i("MainActivityFit", "Total steps: $totalSteps")
                updateStepsUI(steps = totalSteps)
                updateUIVisibility(isConnected = true) // Ensure UI reflects connection
            }
            .addOnFailureListener { e ->
                Log.e("MainActivityFit", "Error reading step count", e)
                updateStepsUI(error = "Error reading steps.")
                // Don't necessarily set isConnected to false here, as permission might still be granted
                // but there was an API error. Let checkAndReadFitDataOnStart handle visibility.
            }
    }
    
    private fun updateStepsUI(steps: Int? = null, error: String? = null) {
        if (error != null) {
            textViewGoogleFitSteps.text = "Today's Steps: $error"
        } else if (steps != null) {
            textViewGoogleFitSteps.text = "Today's Steps: $steps"
        } else {
            textViewGoogleFitSteps.text = "Today's Steps: N/A"
        }
    }

    private fun updateUIVisibility(isConnected: Boolean) {
        if (isConnected) {
            buttonConnectGoogleFit.visibility = View.GONE
            textViewGoogleFitSteps.visibility = View.VISIBLE
        } else {
            buttonConnectGoogleFit.visibility = View.VISIBLE
            // textViewGoogleFitSteps.visibility = View.GONE // Or show "Connect to see steps"
            updateStepsUI(error = "Connect to see steps.")
        }
    }


    private fun setupRecyclerView() {
        recyclerViewActivities = findViewById(R.id.recyclerViewActivities)
        activityListAdapter = ActivityListAdapter { activityLog ->
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(activityLog.timestamp))
            Toast.makeText(this, "Clicked: ${activityLog.type} on $dateStr", Toast.LENGTH_SHORT).show()
        }
        recyclerViewActivities.adapter = activityListAdapter
        recyclerViewActivities.layoutManager = LinearLayoutManager(this)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> true
                R.id.navigation_log_activity -> {
                    startActivity(Intent(this, LogActivityActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_NAME", userName)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupGoalUI() {
        textViewGoalDescription = findViewById(R.id.textViewGoalDescription)
        textViewGoalProgress = findViewById(R.id.textViewGoalProgress)
        buttonSetGoal = findViewById(R.id.buttonSetGoal)

        buttonSetGoal.setOnClickListener {
            startActivity(Intent(this, SetGoalActivity::class.java))
        }
    }

    private fun observeViewModel() {
        dashboardViewModel.allActivities.observe(this) { activities ->
            activities?.let { activityListAdapter.submitList(it) }
        }

        dashboardViewModel.goalIsSet.observe(this) { isSet ->
            updateGoalDisplay(isSet, dashboardViewModel.goalTargetCount.value, dashboardViewModel.goalProgressCount.value)
        }

        dashboardViewModel.goalTargetCount.observe(this) { target ->
            updateGoalDisplay(dashboardViewModel.goalIsSet.value ?: false, target, dashboardViewModel.goalProgressCount.value)
        }

        dashboardViewModel.goalProgressCount.observe(this) { progress ->
            updateGoalDisplay(dashboardViewModel.goalIsSet.value ?: false, dashboardViewModel.goalTargetCount.value, progress)
        }

        dashboardViewModel.workoutSuggestion.observe(this) { suggestion ->
            if (!suggestion.isNullOrBlank()) {
                textViewWorkoutSuggestion.text = suggestion
                cardViewWorkoutSuggestion.visibility = View.VISIBLE
            } else {
                cardViewWorkoutSuggestion.visibility = View.GONE
            }
        }
    }
    
    private fun updateGoalDisplay(isSet: Boolean, target: Int?, progress: Int?) {
        if (isSet && target != null && target > 0) {
            textViewGoalDescription.text = "Goal: Log $target workouts this week"
            val currentProgress = progress ?: 0
            textViewGoalProgress.text = "Progress: $currentProgress/$target workouts"
            if (currentProgress >= target) {
                textViewGoalProgress.append(" - Goal Met! \uD83C\uDF89") // 🎉
                textViewGoalProgress.setTextColor(ContextCompat.getColor(this, R.color.colorSecondary)) // Example: Pink
            } else {
                textViewGoalProgress.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary)) // Default text color
            }
            textViewGoalProgress.visibility = TextView.VISIBLE
        } else {
            textViewGoalDescription.text = "No goal set. Tap 'Set Goal' to start."
            textViewGoalProgress.visibility = TextView.GONE
            textViewGoalProgress.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary)) // Reset color
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.loadGoalData() // Refresh goal data
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            dashboardViewModel.fetchWorkoutSuggestion(userId) // Refresh suggestion
        }
        if (bottomNavigationView.selectedItemId != R.id.navigation_dashboard) {
            bottomNavigationView.selectedItemId = R.id.navigation_dashboard
        }
    }
}
