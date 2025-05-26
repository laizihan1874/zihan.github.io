package com.example.fitnesstrackerapp.user.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.auth.viewmodel.ProfileViewModel
import com.example.fitnesstrackerapp.auth.viewmodel.ProfileViewModelFactory
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import com.example.fitnesstrackerapp.user.model.UserProfile

class FitnessAssessmentActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(application, AppDatabase.getDatabase(applicationContext).userProfileDao())
    }

    // UI Elements
    private lateinit var radioGroupFitnessLevel: RadioGroup
    private lateinit var radioButtonBeginner: RadioButton
    private lateinit var radioButtonIntermediate: RadioButton
    private lateinit var radioButtonAdvanced: RadioButton

    private lateinit var checkboxGoalWeightLoss: CheckBox
    private lateinit var checkboxGoalMuscleGain: CheckBox
    private lateinit var checkboxGoalEndurance: CheckBox
    private lateinit var checkboxGoalGeneralHealth: CheckBox
    private lateinit var checkboxGoalStressRelief: CheckBox
    private lateinit var goalsCheckBoxes: List<CheckBox>

    private lateinit var checkboxActivityRunning: CheckBox
    private lateinit var checkboxActivityCycling: CheckBox
    private lateinit var checkboxActivityGym: CheckBox
    private lateinit var checkboxActivityYoga: CheckBox
    private lateinit var checkboxActivityHiking: CheckBox
    private lateinit var checkboxActivitySwimming: CheckBox
    private lateinit var activitiesCheckBoxes: List<CheckBox>

    private lateinit var editTextAge: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var editTextWeight: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var buttonSaveAssessment: Button

    private val goalMap: Map<CheckBox, String> by lazy {
        mapOf(
            checkboxGoalWeightLoss to "weight_loss",
            checkboxGoalMuscleGain to "muscle_gain",
            checkboxGoalEndurance to "endurance",
            checkboxGoalGeneralHealth to "general_health",
            checkboxGoalStressRelief to "stress_relief"
        )
    }

    private val activityMap: Map<CheckBox, String> by lazy {
        mapOf(
            checkboxActivityRunning to "running",
            checkboxActivityCycling to "cycling",
            checkboxActivityGym to "gym_workout",
            checkboxActivityYoga to "yoga",
            checkboxActivityHiking to "hiking",
            checkboxActivitySwimming to "swimming"
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fitness_assessment)

        supportActionBar?.title = "Fitness Assessment"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeUI()
        setupGenderSpinner()
        observeViewModel()
        setupSaveButton()
    }

    private fun initializeUI() {
        radioGroupFitnessLevel = findViewById(R.id.radioGroupFitnessLevel)
        radioButtonBeginner = findViewById(R.id.radioButtonBeginner)
        radioButtonIntermediate = findViewById(R.id.radioButtonIntermediate)
        radioButtonAdvanced = findViewById(R.id.radioButtonAdvanced)

        checkboxGoalWeightLoss = findViewById(R.id.checkboxGoalWeightLoss)
        checkboxGoalMuscleGain = findViewById(R.id.checkboxGoalMuscleGain)
        checkboxGoalEndurance = findViewById(R.id.checkboxGoalEndurance)
        checkboxGoalGeneralHealth = findViewById(R.id.checkboxGoalGeneralHealth)
        checkboxGoalStressRelief = findViewById(R.id.checkboxGoalStressRelief)
        goalsCheckBoxes = listOf(checkboxGoalWeightLoss, checkboxGoalMuscleGain, checkboxGoalEndurance, checkboxGoalGeneralHealth, checkboxGoalStressRelief)

        checkboxActivityRunning = findViewById(R.id.checkboxActivityRunning)
        checkboxActivityCycling = findViewById(R.id.checkboxActivityCycling)
        checkboxActivityGym = findViewById(R.id.checkboxActivityGym)
        checkboxActivityYoga = findViewById(R.id.checkboxActivityYoga)
        checkboxActivityHiking = findViewById(R.id.checkboxActivityHiking)
        checkboxActivitySwimming = findViewById(R.id.checkboxActivitySwimming)
        activitiesCheckBoxes = listOf(checkboxActivityRunning, checkboxActivityCycling, checkboxActivityGym, checkboxActivityYoga, checkboxActivityHiking, checkboxActivitySwimming)

        editTextAge = findViewById(R.id.editTextAge)
        spinnerGender = findViewById(R.id.spinnerGender)
        editTextWeight = findViewById(R.id.editTextWeight)
        editTextHeight = findViewById(R.id.editTextHeight)
        buttonSaveAssessment = findViewById(R.id.buttonSaveAssessment)
    }
    
    private fun setupGenderSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGender.adapter = adapter
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(this) { userProfile ->
            userProfile?.let { populateForm(it) }
        }

        viewModel.saveAssessmentResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(this, "Assessment Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save assessment.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateForm(profile: UserProfile) {
        when (profile.fitnessLevel) {
            "beginner" -> radioButtonBeginner.isChecked = true
            "intermediate" -> radioButtonIntermediate.isChecked = true
            "advanced" -> radioButtonAdvanced.isChecked = true
        }

        profile.primaryGoals?.forEach { goalKey ->
            goalMap.entries.find { it.value == goalKey }?.key?.isChecked = true
        }
        profile.preferredActivities?.forEach { activityKey ->
            activityMap.entries.find { it.value == activityKey }?.key?.isChecked = true
        }

        profile.age?.let { editTextAge.setText(it.toString()) }
        profile.weightKg?.let { editTextWeight.setText(it.toString()) }
        profile.heightCm?.let { editTextHeight.setText(it.toString()) }
        
        profile.gender?.let { genderValue ->
            val genderArray = resources.getStringArray(R.array.gender_options)
            val position = genderArray.indexOf(genderValue)
            if (position >= 0) {
                spinnerGender.setSelection(position)
            }
        }
    }

    private fun setupSaveButton() {
        buttonSaveAssessment.setOnClickListener {
            val fitnessLevel = when (radioGroupFitnessLevel.checkedRadioButtonId) {
                R.id.radioButtonBeginner -> "beginner"
                R.id.radioButtonIntermediate -> "intermediate"
                R.id.radioButtonAdvanced -> "advanced"
                else -> null
            }

            val selectedGoals = goalsCheckBoxes.filter { it.isChecked }.mapNotNull { goalMap[it] }
            val selectedActivities = activitiesCheckBoxes.filter { it.isChecked }.mapNotNull { activityMap[it] }
            
            val ageStr = editTextAge.text.toString()
            val age = if (ageStr.isNotEmpty()) ageStr.toIntOrNull() else null
            if (ageStr.isNotEmpty() && age == null) {
                editTextAge.error = "Invalid age"; return@setOnClickListener
            }

            val weightStr = editTextWeight.text.toString()
            val weight = if (weightStr.isNotEmpty()) weightStr.toFloatOrNull() else null
             if (weightStr.isNotEmpty() && weight == null) {
                editTextWeight.error = "Invalid weight"; return@setOnClickListener
            }

            val heightStr = editTextHeight.text.toString()
            val height = if (heightStr.isNotEmpty()) heightStr.toFloatOrNull() else null
             if (heightStr.isNotEmpty() && height == null) {
                editTextHeight.error = "Invalid height"; return@setOnClickListener
            }
            
            val gender = if (spinnerGender.selectedItemPosition > 0 || spinnerGender.selectedItem.toString() != "Prefer not to say") {
                 spinnerGender.selectedItem.toString()
            } else {
                null // Store null if "Prefer not to say" or nothing specific is chosen
            }


            viewModel.saveAssessment(
                fitnessLevel,
                selectedGoals.ifEmpty { null },
                selectedActivities.ifEmpty { null },
                age,
                gender,
                weight,
                height
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
