package com.example.fitnesstrackerapp.gamification.logic

import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.gamification.model.AchievementTypes
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class XPCalculatorTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockUserProfileDao: UserProfileDao

    // Using the real LevelingSystem object as it contains pure functions and constants
    private val levelingSystem = LevelingSystem

    private lateinit var xpCalculator: XPCalculator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        xpCalculator = XPCalculator(mockUserProfileDao, levelingSystem)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addXPForActivity new user, Running GPS with distance`() = runTest(testDispatcher) {
        val userId = "newUser1"
        val displayName = "New User"
        val email = "new@example.com"
        val distanceKm = 2.0
        val expectedXPFromActivity = (distanceKm * LevelingSystem.XP_PER_KM_RUNNING_GPS).toLong()
        val expectedInitialLevel = 1
        val expectedNewLevel = LevelingSystem.getLevelForXP(expectedXPFromActivity)

        // Path for 2km (approx 0.018 degrees latitude change for 2km)
        val pathPoints = listOf("0.0,0.0", "0.018,0.0")
        val activityLog = ActivityLog(
            id = 1L, type = AchievementChecker.RUNNING_GPS_TYPE, timestamp = System.currentTimeMillis(),
            durationMillis = 30 * 60000, caloriesBurned = 200, pathPoints = pathPoints
        )

        // Simulate new user - profile doesn't exist
        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(null)

        xpCalculator.addXPForActivity(userId, activityLog, displayName, email)

        // Verify profile creation
        val expectedNewProfile = UserProfile(userId, displayName, email, 0L, 1) // Initial state before XP
        verify(mockUserProfileDao).upsertUserProfile(expectedNewProfile)

        // Verify XP and level update
        verify(mockUserProfileDao).updateUserXPAndLevel(userId, expectedXPFromActivity, expectedNewLevel)
    }

    @Test
    fun `addXPForActivity existing user, Manual Workout`() = runTest(testDispatcher) {
        val userId = "existingUser1"
        val initialXP = 500L
        val initialLevel = LevelingSystem.getLevelForXP(initialXP) // Should be 1
        val existingProfile = UserProfile(userId, "Existing User", "exist@example.com", initialXP, initialLevel)
        
        val activityLog = ActivityLog(
            id = 2L, type = "Weight Training", timestamp = System.currentTimeMillis(),
            durationMillis = 60 * 60000, caloriesBurned = 300, pathPoints = emptyList()
        )
        val expectedXPFromActivity = LevelingSystem.XP_PER_MANUAL_WORKOUT_LOGGED
        val newTotalXP = initialXP + expectedXPFromActivity
        val newLevel = LevelingSystem.getLevelForXP(newTotalXP)

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXPForActivity(userId, activityLog, existingProfile.displayName, existingProfile.email)
        
        // Verify no new profile creation if name/email match
        verify(mockUserProfileDao, never()).upsertUserProfile(argThat { it.userId == userId && it.xpPoints == 0L }) 
        // Verify update with new XP and Level
        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, newLevel)
    }
    
    @Test
    fun `addXPForActivity Running GPS but empty pathPoints falls back to manual XP`() = runTest(testDispatcher) {
        val userId = "gpsRunnerNoPath"
        val initialXP = 100L
        val initialLevel = 1
        val existingProfile = UserProfile(userId, "GPS Runner No Path", "gps@example.com", initialXP, initialLevel)

        val activityLog = ActivityLog(
            id = 3L, type = AchievementChecker.RUNNING_GPS_TYPE, timestamp = System.currentTimeMillis(),
            durationMillis = 20 * 60000, caloriesBurned = 150, pathPoints = emptyList() // Empty path
        )
        val expectedXPFromActivity = LevelingSystem.XP_PER_MANUAL_WORKOUT_LOGGED
        val newTotalXP = initialXP + expectedXPFromActivity
        val newLevel = LevelingSystem.getLevelForXP(newTotalXP)

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXPForActivity(userId, activityLog, existingProfile.displayName, existingProfile.email)

        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, newLevel)
    }


    @Test
    fun `addXPForAchievement existing user`() = runTest(testDispatcher) {
        val userId = "achiever1"
        val initialXP = LevelingSystem.getXPForLevel(2) - 100 // XP just before leveling to 2
        val initialLevel = 1 
        val existingProfile = UserProfile(userId, "Achiever One", "achiever@example.com", initialXP, initialLevel)
        
        val achievement = Achievement("TEST_ACH", "Test Achievement", "", "", 1.0, AchievementTypes.FIRST_ACTIVITY_TYPE)
        val expectedXPFromAchievement = LevelingSystem.XP_PER_ACHIEVEMENT_UNLOCKED
        val newTotalXP = initialXP + expectedXPFromAchievement
        val newLevel = LevelingSystem.getLevelForXP(newTotalXP) // Should level up to 2

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXPForAchievement(userId, achievement, existingProfile.displayName, existingProfile.email)

        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, newLevel)
        assertEquals(2, newLevel) // Explicitly check level up
    }

    @Test
    fun `addXPForActivity updates existing user profile if displayName or email changes`() = runTest(testDispatcher) {
        val userId = "userToUpdate"
        val initialXP = 100L
        val initialLevel = 1
        val oldDisplayName = "Old Name"
        val oldEmail = "old@example.com"
        val existingProfile = UserProfile(userId, oldDisplayName, oldEmail, initialXP, initialLevel)

        val newDisplayName = "New Name"
        val newEmail = "new@example.com"
        val activityLog = ActivityLog(
            id = 4L, type = "Walking", timestamp = System.currentTimeMillis(),
            durationMillis = 30 * 60000, caloriesBurned = 100
        )
        val activityXP = LevelingSystem.XP_PER_MANUAL_WORKOUT_LOGGED
        val newTotalXP = initialXP + activityXP
        val newLevel = LevelingSystem.getLevelForXP(newTotalXP)

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXPForActivity(userId, activityLog, newDisplayName, newEmail)

        // Verify profile was upserted with new display name/email but original XP/level before calculation
        val updatedProfileForUpsert = existingProfile.copy(displayName = newDisplayName, email = newEmail)
        verify(mockUserProfileDao).upsertUserProfile(updatedProfileForUpsert)
        
        // Verify XP and level update
        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, newLevel)
    }

    // --- Tests for generic addXP method ---
    @Test
    fun `addXP new user, awards XP and sets level`() = runTest(testDispatcher) {
        val userId = "xpNewUser"
        val displayName = "XP New"
        val email = "xpnew@example.com"
        val xpToAdd = 150
        val expectedInitialLevel = 1 // Default for new user
        val expectedNewLevel = levelingSystem.getLevelForXP(xpToAdd.toLong())

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(null)

        xpCalculator.addXP(userId, xpToAdd, displayName, email)

        val expectedNewProfile = UserProfile(userId, displayName, email, 0L, 1)
        verify(mockUserProfileDao).upsertUserProfile(expectedNewProfile)
        verify(mockUserProfileDao).updateUserXPAndLevel(userId, xpToAdd.toLong(), expectedNewLevel)
    }

    @Test
    fun `addXP existing user, XP gain leads to level up`() = runTest(testDispatcher) {
        val userId = "xpExistingUserLevelUp"
        val initialXP = LevelingSystem.getXPForLevel(2) - 100 // 100 XP away from Level 2
        val initialLevel = 1
        val existingProfile = UserProfile(userId, "XP User", "xp@example.com", initialXP, initialLevel)
        
        val xpToAdd = 150 // This will push user to Level 2
        val newTotalXP = initialXP + xpToAdd
        val expectedNewLevel = 2 // Based on LevelingSystem thresholds

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXP(userId, xpToAdd, existingProfile.displayName, existingProfile.email)

        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, expectedNewLevel)
    }

    @Test
    fun `addXP existing user, XP gain does not lead to level up`() = runTest(testDispatcher) {
        val userId = "xpExistingUserNoLevelUp"
        val initialXP = LevelingSystem.getXPForLevel(2) + 100 // Already Level 2, 100 XP into it
        val initialLevel = 2
        val existingProfile = UserProfile(userId, "XP User NL", "xpnl@example.com", initialXP, initialLevel)

        val xpToAdd = 50
        val newTotalXP = initialXP + xpToAdd
        val expectedNewLevel = 2 // Still Level 2

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXP(userId, xpToAdd, existingProfile.displayName, existingProfile.email)

        verify(mockUserProfileDao).updateUserXPAndLevel(userId, newTotalXP, expectedNewLevel)
    }

    @Test
    fun `addXP with zero or negative xpAmount does nothing`() = runTest(testDispatcher) {
        val userId = "xpNoChangeUser"
        val initialXP = 500L
        val initialLevel = 1
        val existingProfile = UserProfile(userId, "No Change", "nc@example.com", initialXP, initialLevel)

        `when`(mockUserProfileDao.getUserProfileSuspending(userId)).thenReturn(existingProfile)

        xpCalculator.addXP(userId, 0, existingProfile.displayName, existingProfile.email)
        xpCalculator.addXP(userId, -50, existingProfile.displayName, existingProfile.email)
        
        // Ensure ensureUserProfileExists might be called, but not updateUserXPAndLevel
        // Depending on implementation, upsert might be called if name/email were different, but not relevant here.
        verify(mockUserProfileDao, never()).updateUserXPAndLevel(anyString(), anyLong(), anyInt())
    }
}
