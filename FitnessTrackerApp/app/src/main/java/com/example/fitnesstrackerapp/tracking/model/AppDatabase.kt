package com.example.fitnesstrackerapp.tracking.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.gamification.model.AchievementDao
import com.example.fitnesstrackerapp.gamification.model.AchievementTypes
import com.example.fitnesstrackerapp.gamification.model.UserAchievement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import com.example.fitnesstrackerapp.goal.model.UserGoal // Added
import com.example.fitnesstrackerapp.goal.model.UserGoalDao // Added
import com.example.fitnesstrackerapp.challenge.model.Challenge // Added
import com.example.fitnesstrackerapp.challenge.model.ChallengeDao // Added
import com.example.fitnesstrackerapp.challenge.model.ChallengeTypes // Added
import com.example.fitnesstrackerapp.challenge.model.UserChallenge // Added

@Database(entities = [
    ActivityLog::class, Achievement::class, UserAchievement::class, 
    UserProfile::class, UserGoal::class, Challenge::class, UserChallenge::class
], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userGoalDao(): UserGoalDao 
    abstract fun challengeDao(): ChallengeDao // Added

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_tracker_database"
                )
                .addCallback(AppDatabaseCallback(context)) 
                .fallbackToDestructiveMigration() 
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialAchievements(database.achievementDao())
                    populateInitialChallenges(database.challengeDao()) // Added call
                }
            }
        }

        suspend fun populateInitialAchievements(achievementDao: AchievementDao) {
            val achievements = listOf(
                Achievement(
                    id = "FIRST_RUN",
                    name = "First Run",
                    description = "Log your first 'Running (GPS)' activity",
                    iconName = "ic_achievement_generic",
                    targetValue = 1.0, // 1st activity of this type
                    type = AchievementTypes.FIRST_ACTIVITY_TYPE // Type implies "Running (GPS)" for this ID
                ),
                Achievement(
                    id = "RUN_5KM",
                    name = "5km Runner",
                    description = "Complete a 5km run in a single 'Running (GPS)' activity",
                    iconName = "ic_achievement_generic",
                    targetValue = 5.0, // 5 km
                    type = AchievementTypes.DISTANCE_SINGLE_ACTIVITY // Type implies "Running (GPS)"
                ),
                Achievement(
                    id = "MARATHON_RUNNER",
                    name = "Marathon Runner",
                    description = "Complete a 42.195km 'Running (GPS)' run",
                    iconName = "ic_achievement_generic",
                    targetValue = 42.195, // km
                    type = AchievementTypes.DISTANCE_SINGLE_ACTIVITY
                ),
                Achievement(
                    id = "WEEKLY_WARRIOR",
                    name = "Weekly Warrior",
                    description = "Log 5 activities in one week",
                    iconName = "ic_achievement_generic",
                    targetValue = 5.0, // 5 activities
                    type = AchievementTypes.TOTAL_ACTIVITIES_WINDOW // Implicitly 7 days for "weekly"
                ),
                Achievement(
                    id = "EARLY_BIRD",
                    name = "Early Bird",
                    description = "Log an activity before 7 AM",
                    iconName = "ic_achievement_generic",
                    targetValue = 7.0, // Before 7.00 hours
                    type = AchievementTypes.TIME_OF_DAY_ACTIVITY_LOGGED
                )
            )
            achievementDao.insertAchievements(achievements)
        }
        
        suspend fun populateInitialChallenges(challengeDao: ChallengeDao) { // Added method
            val challenges = listOf(
                Challenge(
                    id = "DAILY_STEPS_5K",
                    name = "5k Steps Daily",
                    description = "Achieve 5,000 steps today!",
                    challengeType = ChallengeTypes.STEPS,
                    activityTypeFilter = null, // Applies to all activities contributing steps
                    targetValue = 5000.0,
                    durationDays = 1,
                    xpReward = 100,
                    isActiveGlobally = true
                ),
                Challenge(
                    id = "WEEKLY_RUN_10KM",
                    name = "10km Run Weekly",
                    description = "Run a total of 10km this week.",
                    challengeType = ChallengeTypes.DISTANCE_KM,
                    activityTypeFilter = "Running (GPS)", // Specific to "Running (GPS)" type
                    targetValue = 10.0, // 10 km
                    durationDays = 7,
                    xpReward = 250,
                    isActiveGlobally = true
                ),
                Challenge(
                    id = "LOG_YOGA_3TIMES",
                    name = "Yoga thrice weekly",
                    description = "Log 3 Yoga sessions this week.",
                    challengeType = ChallengeTypes.LOG_ACTIVITY_COUNT,
                    activityTypeFilter = "Yoga", // Specific to "Yoga" type
                    targetValue = 3.0, // 3 sessions
                    durationDays = 7,
                    xpReward = 150,
                    isActiveGlobally = true
                )
            )
            challengeDao.insertChallenges(challenges)
        }
    }
}
