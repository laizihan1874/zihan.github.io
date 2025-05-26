package com.example.fitnesstrackerapp.tracking.model

import org.junit.Assert.*
import org.junit.Test

class ActivityLogTest {

    @Test
    fun `create ActivityLog with specific values`() {
        val id = 1L
        val type = "Running"
        val timestamp = System.currentTimeMillis()
        val durationMillis = 30 * 60 * 1000L // 30 minutes
        val caloriesBurned = 350
        val notes = "Morning run in the park"

        val activityLog = ActivityLog(
            id = id,
            type = type,
            timestamp = timestamp,
            durationMillis = durationMillis,
            caloriesBurned = caloriesBurned,
            notes = notes
        )

        assertEquals(id, activityLog.id)
        assertEquals(type, activityLog.type)
        assertEquals(timestamp, activityLog.timestamp)
        assertEquals(durationMillis, activityLog.durationMillis)
        assertEquals(caloriesBurned, activityLog.caloriesBurned)
        assertEquals(notes, activityLog.notes)
    }

    @Test
    fun `create ActivityLog with default id and null notes`() {
        val type = "Cycling"
        val timestamp = System.currentTimeMillis() - (60 * 60 * 1000L) // 1 hour ago
        val durationMillis = 45 * 60 * 1000L // 45 minutes
        val caloriesBurned = 400

        val activityLog = ActivityLog(
            // id is auto-generated, so we don't set it here for this "default" test case
            // when Room inserts it, it would be 0 then assigned.
            // For a simple POJO test, we can still test default as 0 if we construct it that way.
            id = 0L, // Explicitly testing the default constructor behavior (or what Room might start with)
            type = type,
            timestamp = timestamp,
            durationMillis = durationMillis,
            caloriesBurned = caloriesBurned,
            notes = null
        )

        assertEquals(0L, activityLog.id) // Default value if not specified and not yet in DB
        assertEquals(type, activityLog.type)
        assertEquals(timestamp, activityLog.timestamp)
        assertEquals(durationMillis, activityLog.durationMillis)
        assertEquals(caloriesBurned, activityLog.caloriesBurned)
        assertNull(activityLog.notes)
    }
}
