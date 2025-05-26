package com.example.fitnesstrackerapp.utils

object Constants {
    // Service Actions
    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE" // For future use
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    // Notification Configuration
    const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Location Tracking"
    const val NOTIFICATION_ID = 1 // Must be > 0

    // Location Broadcast
    const val ACTION_LOCATION_BROADCAST = "ACTION_LOCATION_BROADCAST"
    const val EXTRA_LOCATION = "EXTRA_LOCATION" // Key for passing Location object
    const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
    const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"


    // Location Request
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    const val FASTEST_LOCATION_INTERVAL = 2000L // 2 seconds

    // Permissions
    const val REQUEST_CODE_LOCATION_PERMISSION = 1001
}
