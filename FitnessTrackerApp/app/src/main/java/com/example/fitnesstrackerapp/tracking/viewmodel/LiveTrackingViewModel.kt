package com.example.fitnesstrackerapp.tracking.viewmodel

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LiveTrackingViewModel : ViewModel() {

    // Timer related
    private val _elapsedTime = MutableLiveData<Long>(0L) // Time in milliseconds
    val elapsedTime: LiveData<Long> = _elapsedTime

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private var timerJob: Job? = null
    private var timeWhenPaused: Long = 0L
    private var startTime: Long = 0L
    var trackingStartTimeMillis: Long = 0L
        private set // Allow external read but private set

    // Location and Metrics related
    private val _locationPointsList = MutableLiveData<MutableList<Pair<Double, Double>>>(mutableListOf())
    val locationPointsList: LiveData<MutableList<Pair<Double, Double>>> = _locationPointsList // For potential map display later

    private val _totalDistanceMeters = MutableLiveData<Float>(0f)
    val totalDistanceMeters: LiveData<Float> = _totalDistanceMeters

    private val _currentPaceMinutesPerKm = MutableLiveData<Float>(0f) // Pace in minutes per kilometer
    val currentPaceMinutesPerKm: LiveData<Float> = _currentPaceMinutesPerKm

    // --- Timer Methods ---
    fun startTracking() {
        if (_isTracking.value == true) return // Already tracking

        if (timeWhenPaused == 0L && (_elapsedTime.value ?: 0L) == 0L) { // Fresh start
            trackingStartTimeMillis = System.currentTimeMillis()
            // Reset path for a completely new session if it wasn't fully reset by a previous stop
            _locationPointsList.value = mutableListOf() 
            _totalDistanceMeters.value = 0f
            _currentPaceMinutesPerKm.value = 0f
        }

        _isTracking.value = true
        startTime = SystemClock.elapsedRealtime() - timeWhenPaused // Adjust for pause time

        timerJob?.cancel() // Cancel any existing job
        timerJob = viewModelScope.launch {
            while (_isTracking.value == true) {
                val currentElapsedTime = SystemClock.elapsedRealtime() - startTime
                _elapsedTime.postValue(currentElapsedTime)
                // Update pace/speed here as well, as it depends on time
                calculateMetrics()
                delay(1000) // Update time and metrics every second
            }
        }
    }

    fun pauseTracking() {
        if (_isTracking.value == false) return // Not tracking or already paused

        timerJob?.cancel()
        _isTracking.value = false
        timeWhenPaused = _elapsedTime.value ?: 0L
    }

    fun stopTracking() {
        timerJob?.cancel()
        _isTracking.value = false
        timeWhenPaused = 0L
        _elapsedTime.value = 0L
        trackingStartTimeMillis = 0L // Reset start time

        // Reset location data and metrics
        _locationPointsList.value = mutableListOf()
        _totalDistanceMeters.value = 0f
        _currentPaceMinutesPerKm.value = 0f
    }

    // --- Location and Metrics Methods ---
    fun addLocationPoint(latitude: Double, longitude: Double) {
        val newPoint = Pair(latitude, longitude)
        _locationPointsList.value?.add(newPoint)
        _locationPointsList.postValue(_locationPointsList.value) // Notify observers of list change

        calculateMetrics()
    }

    private fun calculateMetrics() {
        val points = _locationPointsList.value ?: return
        if (points.size < 2) {
            _totalDistanceMeters.postValue(0f)
            _currentPaceMinutesPerKm.postValue(0f)
            return
        }

        var newTotalDistance = 0f
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i+1]
            val results = FloatArray(1)
            Location.distanceBetween(p1.first, p1.second, p2.first, p2.second, results)
            newTotalDistance += results[0]
        }
        _totalDistanceMeters.postValue(newTotalDistance)

        val elapsedTimeMillis = _elapsedTime.value ?: 0L
        if (newTotalDistance > 0f && elapsedTimeMillis > 0L) {
            val elapsedTimeSeconds = elapsedTimeMillis / 1000.0
            val totalDistanceKm = newTotalDistance / 1000.0

            // Pace in minutes per km
            val paceMinPerKm = (elapsedTimeSeconds / 60.0) / totalDistanceKm
            _currentPaceMinutesPerKm.postValue(paceMinPerKm.toFloat())

            // Speed in km/h (optional to add LiveData for this)
            // val speedKmph = totalDistanceKm / (elapsedTimeSeconds / 3600.0)
            // _currentSpeedKmph.postValue(speedKmph.toFloat())
        } else {
            _currentPaceMinutesPerKm.postValue(0f)
        }
    }


    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel() // Ensure coroutine is cancelled when ViewModel is cleared
    }

    // Helper to format time, can be used in Activity or data binding
    fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
