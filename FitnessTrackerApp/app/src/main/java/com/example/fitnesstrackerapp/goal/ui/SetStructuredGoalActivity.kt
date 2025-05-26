package com.example.fitnesstrackerapp.goal.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.goal.model.GoalType
import com.example.fitnesstrackerapp.goal.model.UserGoal
import com.example.fitnesstrackerapp.goal.viewmodel.UserGoalsViewModel
import com.example.fitnesstrackerapp.goal.viewmodel.UserGoalsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class SetStructuredGoalActivity : AppCompatActivity() {

    private lateinit var spinnerGoalType: Spinner
    private lateinit var editTextTargetValue: EditText
    private lateinit var textViewTargetValueUnit: TextView
    private lateinit var layoutTargetDate: LinearLayout
    private lateinit var textViewTargetDateLabel: TextView
    private lateinit var textViewTargetDate: TextView
    private lateinit var buttonPickTargetDate: Button
    private lateinit var buttonClearTargetDate: Button
    private lateinit var textViewActivityFilterLabel: TextView
    private lateinit var spinnerActivityFilter: Spinner
    private lateinit var buttonSaveGoal: Button

    private var selectedTargetDateMillis: Long? = null
    private val calendar = Calendar.getInstance()

    private val viewModel: UserGoalsViewModel by viewModels {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_LONG).show()
            finish()
            object : ViewModelProvider.Factory { // Should not be reached
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("User not logged in for SetStructuredGoalActivity")
                }
            }
        } else {
            UserGoalsViewModelFactory(application, userId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_structured_goal)

        supportActionBar?.title = "Set New Goal"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (FirebaseAuth.getInstance().currentUser?.uid == null) return // Safety check

        initializeUI()
        populateGoalTypeSpinner()
        populateActivityFilterSpinner() // Populate it always, just control visibility
        setupListeners()
        observeViewModel()
    }

    private fun initializeUI() {
        spinnerGoalType = findViewById(R.id.spinnerGoalType)
        editTextTargetValue = findViewById(R.id.editTextTargetValue)
        textViewTargetValueUnit = findViewById(R.id.textViewTargetValueUnit)
        layoutTargetDate = findViewById(R.id.layoutTargetDate)
        textViewTargetDateLabel = findViewById(R.id.textViewTargetDateLabel)
        textViewTargetDate = findViewById(R.id.textViewTargetDate)
        buttonPickTargetDate = findViewById(R.id.buttonPickTargetDate)
        buttonClearTargetDate = findViewById(R.id.buttonClearTargetDate)
        textViewActivityFilterLabel = findViewById(R.id.textViewActivityFilterLabel)
        spinnerActivityFilter = findViewById(R.id.spinnerActivityFilter)
        buttonSaveGoal = findViewById(R.id.buttonSaveGoal)
    }

    private fun populateGoalTypeSpinner() {
        val goalTypeNames = GoalType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goalTypeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGoalType.adapter = adapter
    }
    
    private fun populateActivityFilterSpinner() {
        // Using activity_types array used for manual logging, excluding "Select Activity Type"
        val activities = resources.getStringArray(R.array.activity_types).filter { it != "Select Activity Type" }.toMutableList()
        activities.add(0, "Any Activity") // Add "Any" as an option

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerActivityFilter.adapter = adapter
        spinnerActivityFilter.setSelection(0) // Default to "Any Activity"
    }


    private fun setupListeners() {
        spinnerGoalType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGoalType = GoalType.values()[position]
                textViewTargetValueUnit.text = selectedGoalType.unit

                if (selectedGoalType.requiresTargetDate) {
                    layoutTargetDate.visibility = View.VISIBLE
                    textViewTargetDateLabel.visibility = View.VISIBLE
                } else {
                    layoutTargetDate.visibility = View.GONE
                    textViewTargetDateLabel.visibility = View.GONE
                    selectedTargetDateMillis = null // Clear date if not required
                    updateTargetDateDisplay()
                }

                if (selectedGoalType.requiresActivityFilter) {
                    spinnerActivityFilter.visibility = View.VISIBLE
                    textViewActivityFilterLabel.visibility = View.VISIBLE
                } else {
                    spinnerActivityFilter.visibility = View.GONE
                    textViewActivityFilterLabel.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buttonPickTargetDate.setOnClickListener { showDatePickerDialog() }
        buttonClearTargetDate.setOnClickListener {
            selectedTargetDateMillis = null
            updateTargetDateDisplay()
            buttonClearTargetDate.visibility = View.GONE
        }

        buttonSaveGoal.setOnClickListener { saveGoal() }
    }
    
    private fun showDatePickerDialog() {
        val currentCal = Calendar.getInstance()
        selectedTargetDateMillis?.let { currentCal.timeInMillis = it }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 23) // End of day
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                selectedTargetDateMillis = calendar.timeInMillis
                updateTargetDateDisplay()
                buttonClearTargetDate.visibility = View.VISIBLE
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() // Target date must be in future
        datePickerDialog.show()
    }

    private fun updateTargetDateDisplay() {
        if (selectedTargetDateMillis != null) {
            val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            textViewTargetDate.text = sdf.format(Date(selectedTargetDateMillis!!))
        } else {
            textViewTargetDate.text = "Not Set"
        }
    }

    private fun saveGoal() {
        val selectedGoalTypeEnum = GoalType.values()[spinnerGoalType.selectedItemPosition]
        val targetValueStr = editTextTargetValue.text.toString()

        if (targetValueStr.isEmpty()) {
            editTextTargetValue.error = "Target value cannot be empty"
            return
        }
        val targetValue = targetValueStr.toDoubleOrNull()
        if (targetValue == null || targetValue <= 0) {
            editTextTargetValue.error = "Target must be a positive number"
            return
        }
        
        if (selectedGoalTypeEnum.requiresTargetDate && selectedTargetDateMillis == null) {
            Toast.makeText(this, "Please select a target date for this goal type.", Toast.LENGTH_LONG).show()
            return
        }


        val activityFilter = if (selectedGoalTypeEnum.requiresActivityFilter) {
            val selectedFilter = spinnerActivityFilter.selectedItem.toString()
            if (selectedFilter == "Any Activity") null else selectedFilter
        } else {
            null
        }
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Should not be null here

        // Initial current value (e.g., for WEIGHT_TARGET, could fetch current weight)
        var initialCurrentValue = 0.0
        if (selectedGoalTypeEnum == GoalType.WEIGHT_TARGET) {
            // Placeholder: Fetch current weight from UserProfile if available via ViewModel
            // For now, default to 0, meaning user needs to log weight to make progress.
            // Or, set target as current and user logs deviations. For simplicity, 0.
        }


        val userGoal = UserGoal(
            userId = userId,
            goalType = selectedGoalTypeEnum.name, // Store enum name as string
            targetValue = targetValue,
            currentValue = initialCurrentValue,
            startDate = System.currentTimeMillis(),
            targetDate = selectedTargetDateMillis,
            lastUpdated = System.currentTimeMillis(),
            isActive = true,
            isCompleted = false,
            activityTypeFilter = activityFilter
        )
        viewModel.saveGoal(userGoal)
    }

    private fun observeViewModel() {
        viewModel.statusMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (message.contains("successfully")) {
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
