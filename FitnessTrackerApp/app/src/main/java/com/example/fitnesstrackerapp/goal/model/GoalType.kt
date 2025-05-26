package com.example.fitnesstrackerapp.goal.model

enum class GoalType(
    val displayName: String,
    val unit: String,
    val requiresActivityFilter: Boolean = false,
    val requiresTargetDate: Boolean = true // Most goals will have a target date, can be overridden
) {
    WEIGHT_TARGET("Weight Target", "kg", requiresTargetDate = false), // Weight target is ongoing until changed
    WEEKLY_DISTANCE_RUN("Weekly Running Distance", "km", true),
    WEEKLY_DURATION_CYCLE("Weekly Cycling Duration", "hours", true),
    DAILY_STEP_COUNT("Daily Step Count", "steps");
    // Add more goal types as needed, e.g.,
    // MONTHLY_TOTAL_ACTIVITIES("Monthly Activity Count", "activities"),
    // SPECIFIC_ACTIVITY_DURATION("Single Activity Duration", "minutes", true)

    companion object {
        fun fromDisplayName(displayName: String): GoalType? {
            return values().find { it.displayName == displayName }
        }
    }
}
