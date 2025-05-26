package com.example.fitnesstrackerapp.gamification.logic

object LevelingSystem {

    // XP Rules
    const val XP_PER_KM_RUNNING_GPS = 50L
    const val XP_PER_KM_CYCLING_GPS = 30L // For "Cycling (GPS)"
    const val XP_PER_KM_WALKING = 60L     // New
    const val XP_PER_MINUTE_WALKING = 3L  // New (for non-GPS)
    const val XP_PER_KM_HIKING = 70L      // New
    const val XP_PER_MINUTE_HIKING = 4L   // New (for non-GPS)
    
    const val XP_PER_MINUTE_SWIMMING = 8L
    const val XP_PER_MINUTE_WEIGHT_TRAINING = 5L
    const val XP_PER_MINUTE_YOGA = 4L
    const val XP_PER_MINUTE_HIIT = 10L
    const val XP_PER_MINUTE_PILATES = 4L
    const val XP_PER_MINUTE_TEAM_SPORT = 7L
    const val XP_PER_MINUTE_DANCING = 6L
    const val XP_PER_MINUTE_MARTIAL_ARTS = 8L
    
    const val XP_PER_GENERAL_ACTIVITY_LOGGED = 75L // Fallback for "General Workout", "Other", or unspecified types
    const val XP_PER_ACHIEVEMENT_UNLOCKED = 250L

    // Predefined XP thresholds for levels (Level 1 is 0 XP)
    // Level -> XP required to reach this level (cumulative)
    private val levelXpThresholds: Map<Int, Long> = mapOf(
        1 to 0L,
        2 to 1000L,
        3 to 2500L, // 1000 (for L2) + 1500
        4 to 5000L, // 2500 (for L3) + 2500
        5 to 8000L, // 5000 (for L4) + 3000
        6 to 12000L, // 8000 (for L5) + 4000
        7 to 17000L,
        8 to 23000L,
        9 to 30000L,
        10 to 40000L
        // Add more levels as needed
    )

    /**
     * Gets the total XP required to reach a specific level.
     * For level 1, it's 0.
     */
    fun getXPForLevel(level: Int): Long {
        return levelXpThresholds[level] ?: Long.MAX_VALUE // Return max if level undefined, effectively capping
    }

    /**
     * Determines the user's current level based on their total XP.
     */
    fun getLevelForXP(xp: Long): Int {
        var currentLevel = 1
        // Iterate from highest defined level downwards
        val sortedLevels = levelXpThresholds.keys.sortedDescending()
        for (level in sortedLevels) {
            if (xp >= (levelXpThresholds[level] ?: Long.MAX_VALUE)) {
                currentLevel = level
                break
            }
        }
        // If XP is less than threshold for level 2, they are level 1.
        // The loop handles this by finding the highest level whose threshold is met.
        // If xp is less than levelXpThresholds[2], it will eventually pick level 1.
        if (xp < (levelXpThresholds[2] ?: 1000L)) { // Default to 1 if below L2 threshold
            return 1
        }
        return currentLevel
    }
}
