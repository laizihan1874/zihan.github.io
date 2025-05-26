package com.example.fitnesstrackerapp.tracking.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.tracking.service.LocationTrackingService
import com.example.fitnesstrackerapp.tracking.viewmodel.LiveTrackingViewModel
import com.example.fitnesstrackerapp.tracking.viewmodel.LogActivityViewModel // Import for saving
import com.example.fitnesstrackerapp.utils.Constants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText


class LiveTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private val liveTrackingViewModel: LiveTrackingViewModel by viewModels()
    private val logActivityViewModel: LogActivityViewModel by viewModels() // For saving
    private var map: GoogleMap? = null
    private lateinit var mapViewLive: MapView

    private lateinit var chronometerDuration: TextView
    private lateinit var textViewDistance: TextView
    private lateinit var textViewPace: TextView
    // private lateinit var textViewCaloriesLive: TextView // Calories still placeholder
    private lateinit var buttonStartTracking: Button
    private lateinit var buttonPauseTracking: Button
    private lateinit var buttonStopTracking: Button

    private var locationServiceIntent: Intent? = null

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == Constants.ACTION_LOCATION_BROADCAST) {
                    val latitude = it.getDoubleExtra(Constants.EXTRA_LATITUDE, 0.0)
                    val longitude = it.getDoubleExtra(Constants.EXTRA_LONGITUDE, 0.0)
                    Log.d("LiveTrackingActivity", "Location Received: Lat: $latitude, Lon: $longitude")
                    if (latitude != 0.0 && longitude != 0.0) { // Basic check for valid data
                        liveTrackingViewModel.addLocationPoint(latitude, longitude)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)
        supportActionBar?.title = "Live Activity Tracking"

        mapViewLive = findViewById(R.id.mapViewLive)
        mapViewLive.onCreate(savedInstanceState)
        mapViewLive.getMapAsync(this)

        chronometerDuration = findViewById(R.id.chronometerDuration)
        textViewDistance = findViewById(R.id.textViewDistance)
        textViewPace = findViewById(R.id.textViewPace)
        // textViewCaloriesLive = findViewById(R.id.textViewCaloriesLive)
        buttonStartTracking = findViewById(R.id.buttonStartTracking)
        buttonPauseTracking = findViewById(R.id.buttonPauseTracking)
        buttonStopTracking = findViewById(R.id.buttonStopTracking)

        locationServiceIntent = Intent(this, LocationTrackingService::class.java)

        // Observe Timer
        liveTrackingViewModel.elapsedTime.observe(this) { timeInMillis ->
            chronometerDuration.text = liveTrackingViewModel.formatTime(timeInMillis)
        }

        // Observe Tracking State for button updates
        liveTrackingViewModel.isTracking.observe(this) { isTracking ->
            updateButtonStates(isTracking)
        }
        
        // Observe Metrics
        liveTrackingViewModel.totalDistanceMeters.observe(this) { distanceMeters ->
            val distanceKm = distanceMeters / 1000f
            textViewDistance.text = String.format("%.2f km", distanceKm)
        }

        liveTrackingViewModel.currentPaceMinutesPerKm.observe(this) { paceMinPerKm ->
            if (paceMinPerKm > 0f && !paceMinPerKm.isInfinite() && !paceMinPerKm.isNaN()) {
                val minutes = paceMinPerKm.toInt()
                val seconds = ((paceMinPerKm - minutes) * 60).toInt()
                textViewPace.text = String.format("%02d:%02d min/km", minutes, seconds)
            } else {
                textViewPace.text = "00:00 min/km"
            }
        }

        liveTrackingViewModel.locationPointsList.observe(this) { pathPoints ->
            updateMapWithPath(pathPoints)
        }
        
        updateButtonStates(false) // Initial state for buttons

        buttonStartTracking.setOnClickListener {
            if (checkLocationPermission()) {
                liveTrackingViewModel.startTracking()
                sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
            } else {
                requestLocationPermission()
            }
        }

        buttonPauseTracking.setOnClickListener {
            liveTrackingViewModel.pauseTracking()
            // sendCommandToService(Constants.ACTION_PAUSE_SERVICE) // If service needs to know about pause
        }

        buttonStopTracking.setOnClickListener {
            // Order is important: First, stop the ViewModel's timer and data collection.
            liveTrackingViewModel.stopTracking() 
            // Then, tell the service to stop location updates and shut down.
            sendCommandToService(Constants.ACTION_STOP_SERVICE)
            // Then, prompt for calories and save.
            promptForCaloriesAndSaveTrack()
        }
    }
    
    private fun updateButtonStates(isTracking: Boolean) {
        buttonStartTracking.isEnabled = !isTracking
        buttonPauseTracking.isEnabled = isTracking
        // Stop button should be enabled if tracking is active OR if it was active and is now paused
        // The ViewModel's elapsedTime can help determine if there's a resumable state.
        buttonStopTracking.isEnabled = isTracking || (liveTrackingViewModel.elapsedTime.value ?: 0L) > 0L
    }


    private fun sendCommandToService(action: String) {
        locationServiceIntent?.let {
            it.action = action
            ContextCompat.startForegroundService(this, it)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Constants.REQUEST_CODE_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, user might click start again or we can auto-start
                Toast.makeText(this, "Permission granted. Click Start again.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Location permission denied. Cannot track activity.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(Constants.ACTION_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        super.onPause()
        mapViewLive.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }

    override fun onStop() {
        super.onStop()
        mapViewLive.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewLive.onLowMemory()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mapViewLive.onDestroy()
        // Service stop logic remains as is (explicit user action or if app is fully killed)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewLive.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Optionally, enable user's current location blue dot if desired
        // map?.isMyLocationEnabled = true // Requires permission check again or ensure it's already granted
        updateMapWithPath(liveTrackingViewModel.locationPointsList.value ?: emptyList())
    }

    private fun updateMapWithPath(pathPoints: List<Pair<Double, Double>>) {
        map?.clear() // Clear previous polylines and markers

        if (pathPoints.isNotEmpty()) {
            val latLngPoints = pathPoints.map { LatLng(it.first, it.second) }
            
            val polylineOptions = PolylineOptions()
                .color(Color.RED)
                .width(10f)
                .addAll(latLngPoints)
            map?.addPolyline(polylineOptions)

            if (latLngPoints.isNotEmpty()) {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPoints.last(), 17f))
            }
        }
    }

    private fun promptForCaloriesAndSaveTrack() {
        val durationMillis = liveTrackingViewModel.elapsedTime.value ?: 0L
        val totalDistanceMeters = liveTrackingViewModel.totalDistanceMeters.value ?: 0f
        val rawPathPoints = liveTrackingViewModel.locationPointsList.value ?: emptyList()
        val startTime = liveTrackingViewModel.trackingStartTimeMillis

        if (durationMillis == 0L || rawPathPoints.isEmpty() || startTime == 0L) {
            Toast.makeText(this, "No track data to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val pathPointsStrings = rawPathPoints.map { "${it.first},${it.second}" }
        val activityType = "Running (GPS)" // Hardcoded for now

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Save Activity")
        builder.setMessage("Enter calories burned (optional):")

        val input = TextInputEditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Calories (e.g., 300)"
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        input.layoutParams = lp
        // Add padding to the EditText
        val paddingDp = 16
        val density = resources.displayMetrics.density
        val paddingPixel = (paddingDp * density).toInt()
        input.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel)

        // Create a container for the EditText to apply margins if needed, or set padding on EditText itself
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(paddingPixel, paddingPixel / 2, paddingPixel, 0)
        container.addView(input, params)
        builder.setView(container)


        builder.setPositiveButton("Save") { dialog, _ ->
            val caloriesStr = input.text.toString()
            val calories = if (caloriesStr.isNotEmpty()) caloriesStr.toIntOrNull() else 0

            if (caloriesStr.isNotEmpty() && calories == null) {
                Toast.makeText(this, "Invalid calorie input.", Toast.LENGTH_SHORT).show()
                // Optionally, don't dismiss dialog or re-prompt
                return@setPositiveButton
            }
            
            logActivityViewModel.saveTrackedActivity(
                activityType = activityType,
                startTimeMillis = startTime,
                durationMillis = durationMillis,
                totalDistanceMeters = totalDistanceMeters,
                pathPointsStrings = pathPointsStrings,
                estimatedCalories = calories ?: 0 // Use 0 if null (empty input)
            )
            dialog.dismiss()
        }
        builder.setNegativeButton("Discard") { dialog, _ ->
            Toast.makeText(this, "Track discarded.", Toast.LENGTH_SHORT).show()
            dialog.cancel()
            finish() // Go back to MainActivity if discarded
        }
        builder.setCancelable(false) // User must choose Save or Discard

        // Observe save result
        // Re-observe here or ensure it's active if declared globally in activity
        // For simplicity, a one-time observer or manage lifecycle carefully
        logActivityViewModel.saveResult.observe(this) { result ->
            val (success, message) = result
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            if (success) {
                finish() // Go back to MainActivity
            }
            // Remove observer if it's a one-time event for this specific save
            logActivityViewModel.saveResult.removeObservers(this) 
        }

        builder.show()
    }
}
