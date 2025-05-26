package com.example.fitnesstrackerapp.tracking.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.viewmodel.ActivityDetailViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ActivityDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_ACTIVITY_ID = "EXTRA_ACTIVITY_ID"
    }

    private val viewModel: ActivityDetailViewModel by viewModels()
    private var map: GoogleMap? = null
    private lateinit var mapViewDetail: MapView

    private lateinit var textViewDetailActivityType: TextView
    private lateinit var textViewDetailDateTime: TextView
    private lateinit var textViewDetailDuration: TextView
    private lateinit var textViewDetailDistance: TextView
    private lateinit var textViewDetailAvgPace: TextView
    private lateinit var textViewDetailCalories: TextView
    private lateinit var textViewDetailNotesLabel: TextView
    private lateinit var textViewDetailNotes: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Activity Details"

        mapViewDetail = findViewById(R.id.mapViewDetail)
        textViewDetailActivityType = findViewById(R.id.textViewDetailActivityType)
        textViewDetailDateTime = findViewById(R.id.textViewDetailDateTime)
        textViewDetailDuration = findViewById(R.id.textViewDetailDuration)
        textViewDetailDistance = findViewById(R.id.textViewDetailDistance)
        textViewDetailAvgPace = findViewById(R.id.textViewDetailAvgPace)
        textViewDetailCalories = findViewById(R.id.textViewDetailCalories)
        textViewDetailNotesLabel = findViewById(R.id.textViewDetailNotesLabel)
        textViewDetailNotes = findViewById(R.id.textViewDetailNotes)
        val buttonLearnMoreAnalytics: Button = findViewById(R.id.buttonLearnMoreAnalytics) // Added

        mapViewDetail.onCreate(savedInstanceState)
        mapViewDetail.getMapAsync(this)

        buttonLearnMoreAnalytics.setOnClickListener { // Added
            val intent = Intent(this, com.example.fitnesstrackerapp.premium.ui.PremiumFeaturesPlaceholderActivity::class.java)
            startActivity(intent)
        }

        val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1L)
        if (activityId != -1L) {
            viewModel.loadActivity(activityId)
        } else {
            // Handle error: No ID passed
            finish() // Or show an error message
            return
        }

        viewModel.activityLog.observe(this) { activityLog ->
            activityLog?.let {
                populateUI(it)
                if (it.pathPoints.isNotEmpty()) {
                    mapViewDetail.visibility = View.VISIBLE
                    updateMapWithPath(it.pathPoints)
                } else {
                    mapViewDetail.visibility = View.GONE
                }
            }
        }
    }

    private fun populateUI(log: ActivityLog) {
        textViewDetailActivityType.text = "Type: ${log.type}"
        textViewDetailDateTime.text = "Date: ${formatTimestampDate(log.timestamp)}"
        textViewDetailDuration.text = "Duration: ${formatDuration(log.durationMillis)}"
        
        val distanceKm = calculateDistanceKmFromPath(log.pathPoints) // Assuming pathPoints is "lat,lng"
        if (distanceKm > 0.0) {
            textViewDetailDistance.text = String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm)
            val avgPaceMinPerKm = calculateAveragePace(log.durationMillis, distanceKm)
            textViewDetailAvgPace.text = "Avg Pace: ${formatPace(avgPaceMinPerKm)}"
            textViewDetailDistance.visibility = View.VISIBLE
            textViewDetailAvgPace.visibility = View.VISIBLE
        } else {
            // Try to get distance if it was a non-GPS activity (not implemented in this log)
            // For now, if no path, assume no distance/pace.
            textViewDetailDistance.visibility = View.GONE
            textViewDetailAvgPace.visibility = View.GONE
        }
        
        textViewDetailCalories.text = "Calories: ${log.caloriesBurned} kcal"
        if (log.notes.isNullOrEmpty()) {
            textViewDetailNotesLabel.visibility = View.GONE
            textViewDetailNotes.visibility = View.GONE
        } else {
            textViewDetailNotesLabel.visibility = View.VISIBLE
            textViewDetailNotes.visibility = View.VISIBLE
            textViewDetailNotes.text = log.notes
        }
    }
    
    // Helper to calculate distance from pathPoints if not stored separately
    private fun calculateDistanceKmFromPath(pathPoints: List<String>): Double {
        if (pathPoints.size < 2) return 0.0
        var totalDistanceMeters = 0f
        val latLngList = pathPoints.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) LatLng(parts[0].toDouble(), parts[1].toDouble()) else null
        }
        for (i in 0 until latLngList.size - 1) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                latLngList[i].latitude, latLngList[i].longitude,
                latLngList[i+1].latitude, latLngList[i+1].longitude,
                results
            )
            totalDistanceMeters += results[0]
        }
        return totalDistanceMeters / 1000.0
    }


    private fun formatTimestampDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%d min %02d sec", minutes, seconds)
        }
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val remainingMinutes = minutes % 60
        return String.format(Locale.getDefault(), "%d hr %02d min %02d sec", hours, remainingMinutes, seconds)
    }

    private fun calculateAveragePace(durationMillis: Long, distanceKm: Double): Double {
        if (distanceKm == 0.0 || durationMillis == 0L) return 0.0
        val durationMinutes = durationMillis / 1000.0 / 60.0
        return durationMinutes / distanceKm // min/km
    }

    private fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0.0 || paceMinPerKm.isInfinite() || paceMinPerKm.isNaN()) {
            return "N/A"
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d min/km", minutes, seconds)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Re-draw path if activityLog data is already loaded
        viewModel.activityLog.value?.let {
            if (it.pathPoints.isNotEmpty()) {
                updateMapWithPath(it.pathPoints)
            }
        }
    }

    private fun updateMapWithPath(pathPointsStrings: List<String>) {
        map?.clear()
        val latLngList = pathPointsStrings.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                try { LatLng(parts[0].toDouble(), parts[1].toDouble()) }
                catch (e: NumberFormatException) { null }
            } else null
        }

        if (latLngList.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .color(Color.BLUE) // Different color for saved path
                .width(10f)
                .addAll(latLngList)
            map?.addPolyline(polylineOptions)

            val builder = LatLngBounds.Builder()
            for (latLng in latLngList) {
                builder.include(latLng)
            }
            val bounds = builder.build()
            val padding = 100 // padding in pixels
            try {
                 map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (e: IllegalStateException) {
                // Map may not be laid out yet, post to view's handler
                mapViewDetail.post {
                    try {
                        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e: IllegalStateException) {
                        // Log or handle if it still fails
                    }
                }
            }
        }
    }

    override fun onStart() { super.onStart(); mapViewDetail.onStart() }
    override fun onResume() { super.onResume(); mapViewDetail.onResume() }
    override fun onPause() { super.onPause(); mapViewDetail.onPause() }
    override fun onStop() { super.onStop(); mapViewDetail.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapViewDetail.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapViewDetail.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewDetail.onSaveInstanceState(outState)
    }
     override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
