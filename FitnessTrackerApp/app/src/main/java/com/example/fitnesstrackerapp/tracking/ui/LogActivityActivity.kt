package com.example.fitnesstrackerapp.tracking.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.tracking.viewmodel.LogActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogActivityActivity : AppCompatActivity() {

    private val logActivityViewModel: LogActivityViewModel by viewModels()

    private lateinit var spinnerActivityType: Spinner
    private lateinit var buttonPickDate: Button
    private lateinit var buttonPickTime: Button
    private lateinit var textViewSelectedDateTime: TextView
    private lateinit var editTextDuration: EditText
    private lateinit var editTextCalories: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var buttonSaveActivity: Button

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_manual)

        spinnerActivityType = findViewById(R.id.spinnerActivityType)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonPickTime = findViewById(R.id.buttonPickTime)
        textViewSelectedDateTime = findViewById(R.id.textViewSelectedDateTime)
        editTextDuration = findViewById(R.id.editTextDuration)
        editTextCalories = findViewById(R.id.editTextCalories)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonSaveActivity = findViewById(R.id.buttonSaveActivity)

        setupSpinner()
        setupDateTimePickers()

        buttonSaveActivity.setOnClickListener {
            saveActivityLog()
        }

        logActivityViewModel.saveResult.observe(this) { result ->
            val (success, message) = result
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            if (success) {
                // Clear form or navigate back
                finish() // For now, just finish the activity
            }
        }
        updateSelectedDateTimeDisplay() // Initial display
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.activity_types, // Will need to create this array in strings.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerActivityType.adapter = adapter
        }
    }

    private fun setupDateTimePickers() {
        buttonPickDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateSelectedDateTimeDisplay()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonPickTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0) // Reset seconds
                    calendar.set(Calendar.MILLISECOND, 0) // Reset milliseconds
                    updateSelectedDateTimeDisplay()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24 hour view
            ).show()
        }
    }

    private fun updateSelectedDateTimeDisplay() {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        textViewSelectedDateTime.text = "Selected: ${sdf.format(calendar.time)}"
    }

    private fun saveActivityLog() {
        val type = spinnerActivityType.selectedItem.toString()
        val timestamp = calendar.timeInMillis
        val durationStr = editTextDuration.text.toString()
        val caloriesStr = editTextCalories.text.toString()
        val notes = editTextNotes.text.toString().trim()

        if (spinnerActivityType.selectedItemPosition == Spinner.INVALID_POSITION || type == "Select Activity Type") { // Assuming a default prompt
            Toast.makeText(this, "Please select an activity type.", Toast.LENGTH_SHORT).show()
            return
        }
        if (timestamp == 0L || textViewSelectedDateTime.text.contains("Not set")) { // Check if date/time hasn't been picked
             Toast.makeText(this, "Please pick a date and time for the activity.", Toast.LENGTH_SHORT).show()
            return
        }


        val durationMinutes = durationStr.toLongOrNull()
        val caloriesBurned = caloriesStr.toIntOrNull()

        if (durationMinutes == null || durationMinutes <= 0) {
            editTextDuration.error = "Please enter a valid duration."
            return
        }
        if (caloriesBurned == null || caloriesBurned <= 0) {
            editTextCalories.error = "Please enter valid calories burned."
            return
        }

        logActivityViewModel.saveActivity(
            type,
            timestamp,
            durationMinutes,
            caloriesBurned,
            if (notes.isEmpty()) null else notes
        )
    }
}
