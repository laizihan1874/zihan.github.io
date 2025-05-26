package com.example.fitnesstrackerapp.tracking.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.tracking.ui.LiveTrackingActivity
import com.example.fitnesstrackerapp.utils.Constants
import com.google.android.gms.location.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isServiceRunning = false

    companion object {
        private const val TAG = "LocationTrackingService"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.locations.forEach { location ->
                    Log.d(TAG, "New Location: ${location.latitude}, ${location.longitude}")
                    val intent = Intent(Constants.ACTION_LOCATION_BROADCAST)
                    intent.putExtra(Constants.EXTRA_LATITUDE, location.latitude)
                    intent.putExtra(Constants.EXTRA_LONGITUDE, location.longitude)
                    // Add other data like accuracy, speed, timestamp if needed
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (!isServiceRunning) {
                        startForegroundService()
                        startLocationUpdates()
                        isServiceRunning = true
                        Log.d(TAG, "Service Started/Resumed")
                    }
                }
                Constants.ACTION_STOP_SERVICE -> {
                    Log.d(TAG, "Service Stopped")
                    stopServiceTracking()
                }
                // Constants.ACTION_PAUSE_SERVICE -> { Log.d(TAG, "Service Paused"); /* TODO */ }
                else -> {}
            }
        }
        return START_NOT_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.FASTEST_LOCATION_INTERVAL)
        }.build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted, cannot start updates.")
            // This should ideally be handled before starting the service.
            // If service starts without permission, it should stop itself or notify error.
            stopSelf()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper() // Or a background looper
        )
        Log.d(TAG, "Location updates started.")
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationProviderClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "Location updates stopped.")
        }
    }
    
    private fun stopServiceTracking() {
        stopLocationUpdates()
        stopForeground(true)
        stopSelf()
        isServiceRunning = false
    }


    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, LiveTrackingActivity::class.java).apply {
            // Add flags or extras if needed when notification is tapped
            // e.g., to resume the activity in a specific state
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false) // Notification should persist while service is running
            .setOngoing(true)     // Indicates a long-running task
            .setSmallIcon(R.drawable.ic_run_placeholder) // Replace with actual run icon
            .setContentTitle("Fitness Tracker")
            .setContentText("Tracking your activity...")
            .setContentIntent(pendingIntent)
            .build()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates() // Ensure updates are stopped
        Log.d(TAG, "Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service for now
    }
}
