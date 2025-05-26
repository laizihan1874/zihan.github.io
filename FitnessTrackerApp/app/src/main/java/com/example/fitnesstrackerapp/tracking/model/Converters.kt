package com.example.fitnesstrackerapp.tracking.model

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPathPointsList(pathPoints: List<String>?): String {
        // Return empty string for null or empty list to store in DB,
        // as split("") can lead to an array with one empty string.
        return pathPoints?.takeIf { it.isNotEmpty() }?.joinToString(separator = "|") ?: ""
    }

    @TypeConverter
    fun toPathPointsList(pathPointsString: String?): List<String> {
        // If the stored string is null or empty, return an empty list.
        return if (pathPointsString.isNullOrEmpty()) {
            emptyList()
        } else {
            pathPointsString.split("|")
        }
    }
}
