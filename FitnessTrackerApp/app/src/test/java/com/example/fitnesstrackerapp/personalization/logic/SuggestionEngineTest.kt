package com.example.fitnesstrackerapp.personalization.logic

import com.example.fitnesstrackerapp.gamification.logic.AchievementChecker // For RUNNING_GPS_TYPE
import com.example.fitnesstrackerapp.goal.model.UserGoalDao
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.ActivityLogDao
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SuggestionEngineTest {

    @Mock
    private lateinit var mockUserProfileDao: UserProfileDao
    @Mock
    private lateinit var mockActivityLogDao: ActivityLogDao
    @Mock
    private lateinit var mockUserGoalDao: UserGoalDao // Not used in current rules, but part of constructor

    private lateinit var suggestionEngine: SuggestionEngine

    private val testUserId = "testUser1"
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        suggestionEngine = SuggestionEngine(mockUserProfileDao, mockActivityLogDao, mockUserGoalDao)
    }

    // Helper to create ActivityLog
    private fun createActivityLog(
        type: String,
        timestamp: Long,
        durationMillis: Long = 0,
        pathPoints: List<String> = emptyList()
    ): ActivityLog {
        return ActivityLog(
            id = timestamp, // Using timestamp as ID for simplicity in test
            userId = testUserId,
            type = type,
            timestamp = timestamp,
            durationMillis = durationMillis,
            caloriesBurned = 0, // Not relevant for these rules
            notes = null,
            pathPoints = pathPoints
        )
    }
    
    // Helper to simulate distance calculation for pathPoints
    // This is a simplified mock of Location.distanceBetween logic for testing purposes
    private fun calculatePathDistanceMeters(pathPoints: List<String>): Float {
        if (pathPoints.size < 2) return 0f
        var totalDistance = 0f
        val latLngList = pathPoints.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) Pair(parts[0].toDouble(), parts[1].toDouble()) else null
        }
        for (i in 0 until latLngList.size - 1) {
            // Simplified: assume 1 degree lat/lon change is approx 111km for test points
            // This is a very rough approximation used ONLY for testing the engine's logic flow
            // Not for testing actual distance calculation accuracy.
            val lat1 = latLngList[i].first
            val lon1 = latLngList[i].second
            val lat2 = latLngList[i+1].first
            val lon2 = latLngList[i+1].second
            // Approx distance for small changes: sqrt( (dLat*111k)^2 + (dLon*111k*cos(lat))^2 )
            // For simplicity, let's assume specific test values make this roughly work
            if (lat1 == 0.0 && lon1 == 0.0 && lat2 == 0.1 && lon2 == 0.0) totalDistance += 11100f // Approx 11.1km
            else if (lat1 == 0.0 && lon1 == 0.0 && lat2 == 0.05 && lon2 == 0.0) totalDistance += 5550f // Approx 5.55km
        }
        return totalDistance
    }


    // --- Test Rest Day Rule ---
    @Test
    fun `getWorkoutSuggestion returns rest day for recent strenuous run (distance)`() = runTest {
        val strenuousRunPath = listOf("0.0,0.0", "0.1,0.0") // Approx 11.1km
        val recentStrenuousActivity = createActivityLog(
            type = AchievementChecker.RUNNING_GPS_TYPE,
            timestamp = now - TimeUnit.HOURS.toMillis(10), // Today
            pathPoints = strenuousRunPath
            // duration will be ignored if distance is met
        )
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(UserProfile(testUserId, "Test User", "test@example.com"))
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentStrenuousActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("rest day") == true)
    }
    
    @Test
    fun `getWorkoutSuggestion returns rest day for recent strenuous run (duration)`() = runTest {
         val recentStrenuousActivity = createActivityLog(
            type = AchievementChecker.RUNNING_GPS_TYPE,
            timestamp = now - TimeUnit.DAYS.toMillis(1), // Yesterday
            durationMillis = TimeUnit.MINUTES.toMillis(70) // > 60 mins
        )
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(UserProfile(testUserId, "Test User", "test@example.com"))
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentStrenuousActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("rest day") == true)
    }


    @Test
    fun `getWorkoutSuggestion no rest day if strenuous activity was 3 days ago`() = runTest {
        val oldStrenuousActivity = createActivityLog(
            type = AchievementChecker.RUNNING_GPS_TYPE,
            timestamp = now - TimeUnit.DAYS.toMillis(3), // 3 days ago
            durationMillis = TimeUnit.MINUTES.toMillis(70)
        )
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(UserProfile(testUserId, "Test User", "test@example.com", preferredActivities = listOf("Cycling")))
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(oldStrenuousActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertFalse(suggestion?.contains("rest day") == true) 
        // Should fall to preferred activity nudge or default
        assertTrue(suggestion?.contains("Cycling") == true)
    }

    // --- Test Preferred Activity Nudge Rule ---
    @Test
    fun `getWorkoutSuggestion suggests preferred activity if not done recently`() = runTest {
        val userProfile = UserProfile(testUserId, "Test User", "test@example.com", preferredActivities = listOf("Cycling", "Yoga"))
        val recentOtherActivity = createActivityLog("Running", now - TimeUnit.DAYS.toMillis(1))
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentOtherActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Cycling") == true)
    }

    @Test
    fun `getWorkoutSuggestion no preferred activity nudge if done recently`() = runTest {
        val userProfile = UserProfile(testUserId, "Test User", "test@example.com", fitnessLevel="intermediate", preferredActivities = listOf("Cycling"))
        val recentCyclingActivity = createActivityLog("Cycling", now - TimeUnit.DAYS.toMillis(1))
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentCyclingActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertFalse(suggestion?.contains("How about a Cycling") == true)
        // Should fall to default since user is intermediate and has recent activity
        assertTrue(suggestion?.contains("Stay active today") == true || suggestion?.contains("Time for a workout") == true)
    }
    
    @Test
    fun `getWorkoutSuggestion no preferred activity nudge if no preferred activities set`() = runTest {
        val userProfile = UserProfile(testUserId, "Test User", "test@example.com", fitnessLevel="intermediate", preferredActivities = emptyList()) // No preferred
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(emptyList())

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        // Should fall to default (generic one, not preferred specific)
        assertTrue(suggestion?.contains("Time for a workout! How about a walk or a quick run?") == true)
    }


    // --- Test Beginner Frequency Rule ---
    @Test
    fun `getWorkoutSuggestion suggests activity for beginner if no activity in 2 days`() = runTest {
        val userProfile = UserProfile(testUserId, "Beginner User", "beg@example.com", fitnessLevel = "beginner", preferredActivities = listOf("Walking"))
        val oldActivity = createActivityLog("Walking", now - TimeUnit.DAYS.toMillis(3)) // 3 days ago
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(oldActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Consistency is key!") == true)
    }

    @Test
    fun `getWorkoutSuggestion no beginner nudge if beginner logged activity yesterday`() = runTest {
        val userProfile = UserProfile(testUserId, "Beginner User", "beg@example.com", fitnessLevel = "beginner", preferredActivities = listOf("Walking"))
        val recentActivity = createActivityLog("Walking", now - TimeUnit.DAYS.toMillis(1)) // Yesterday
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        // Should fall to preferred activity default, not beginner consistency nudge
        assertFalse(suggestion?.contains("Consistency is key!") == true)
        assertTrue(suggestion?.contains("Stay active today! Try a Walking") == true)

    }
    
    @Test
    fun `getWorkoutSuggestion no beginner nudge if user is intermediate`() = runTest {
        val userProfile = UserProfile(testUserId, "Intermediate User", "int@example.com", fitnessLevel = "intermediate", preferredActivities = listOf("Running"))
        // No recent activity
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(emptyList())

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertFalse(suggestion?.contains("Consistency is key!") == true)
        // Should be preferred activity nudge or default
        assertTrue(suggestion?.contains("Running") == true)
    }

    // --- Test Fallback/Default Suggestion ---
    @Test
    fun `getWorkoutSuggestion returns default when no specific rule matches (with preferred)`() = runTest {
        // Intermediate user, recent activity, but preferred activity was also recent
        val userProfile = UserProfile(testUserId, "User", "u@example.com", fitnessLevel = "intermediate", preferredActivities = listOf("Yoga"))
        val recentYoga = createActivityLog("Yoga", now - TimeUnit.HOURS.toMillis(5)) // Today
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentYoga))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Stay active today! Try a Yoga") == true)
    }
    
    @Test
    fun `getWorkoutSuggestion returns generic default when no preferred and no other rules match`() = runTest {
        val userProfile = UserProfile(testUserId, "User", "u@example.com", fitnessLevel = "intermediate", preferredActivities = emptyList())
        val recentActivity = createActivityLog("Gym Workout", now - TimeUnit.HOURS.toMillis(5)) // Today
        
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfile)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(listOf(recentActivity))

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Time for a workout! How about a walk or a quick run?") == true)
    }


    // --- Test Profile/Assessment Data Missing ---
    @Test
    fun `getWorkoutSuggestion returns default if userProfile is null`() = runTest {
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(null)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(emptyList())

        val suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Time for a workout! How about a walk or a quick run?") == true)
    }
    
    @Test
    fun `getWorkoutSuggestion handles missing fitnessLevel or preferredActivities gracefully`() = runTest {
        val userProfileNoLevel = UserProfile(testUserId, "No Level User", "nl@example.com", preferredActivities = listOf("Hiking"))
        val userProfileNoPreferred = UserProfile(testUserId, "No Preferred User", "np@example.com", fitnessLevel = "beginner")
        
        // Scenario 1: Missing fitnessLevel (should not trigger beginner rule, might trigger preferred nudge)
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfileNoLevel)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(emptyList()) // No recent activity
        var suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Hiking") == true) // Preferred activity nudge

        // Scenario 2: Missing preferredActivities (should not trigger preferred nudge, might trigger beginner or default)
        `when`(mockUserProfileDao.getUserProfileSuspending(testUserId)).thenReturn(userProfileNoPreferred)
        `when`(mockActivityLogDao.getRecentActivitiesForUser(testUserId, 5)).thenReturn(emptyList()) // No recent activity for 2+ days
        suggestion = suggestionEngine.getWorkoutSuggestion(testUserId)
        assertTrue(suggestion?.contains("Consistency is key!") == true) // Beginner rule
    }
}
