package com.example.fitnesstrackerapp.gamification.logic

import android.content.Context
import android.widget.Toast
import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.gamification.model.AchievementDao
import com.example.fitnesstrackerapp.gamification.model.AchievementTypes
import com.example.fitnesstrackerapp.gamification.model.UserAchievement
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.ActivityLogDao
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never

// Using MockitoJUnitRunner for Mockito mocks
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AchievementCheckerTest {

    private val testDispatcher = StandardTestDispatcher() // Use StandardTestDispatcher

    @Mock
    private lateinit var mockAchievementDao: AchievementDao
    @Mock
    private lateinit var mockActivityLogDao: ActivityLogDao // Not used in FIRST_RUN, but part of constructor
    @Mock
    private lateinit var mockUserProfileDao: UserProfileDao // For XPCalculator through AchievementChecker
    @Mock
    private lateinit var mockXPCalculator: XPCalculator
    @Mock
    private lateinit var mockContext: Context

    // For Firebase Auth mocking
    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth
    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    private lateinit var achievementChecker: AchievementChecker

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock Firebase Auth
        mockkStatic(FirebaseAuth::class)
        `when`(FirebaseAuth.getInstance()).thenReturn(mockFirebaseAuth)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.uid).thenReturn("testUserId")
        `when`(mockFirebaseUser.displayName).thenReturn("Test User")
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        
        // Mock Toast (conceptual, won't actually show)
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<CharSequence>(), any()) } returns mockk(relaxed = true)


        achievementChecker = AchievementChecker(
            mockAchievementDao,
            mockActivityLogDao,
            mockUserProfileDao, // Now needed by constructor
            mockXPCalculator,
            mockContext
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic(Toast::class)
    }

    @Test
    fun `checkAchievements FIRST_RUN new user logs Running GPS activity`() = runTest(testDispatcher) {
        val userId = "testUserId"
        val newActivityLog = ActivityLog(
            id = 1L, type = AchievementChecker.RUNNING_GPS_TYPE, timestamp = System.currentTimeMillis(),
            durationMillis = 30 * 60000, caloriesBurned = 300, pathPoints = listOf("0,0","1,1")
        )
        val firstRunAchievement = Achievement(
            id = AchievementChecker.FIRST_RUN_ID, name = "First Run", description = "",
            iconName = "", targetValue = 1.0, type = AchievementTypes.FIRST_ACTIVITY_TYPE
        )

        // Simulate achievement not yet unlocked
        `when`(mockAchievementDao.getSpecificUserAchievement(userId, AchievementChecker.FIRST_RUN_ID))
            .thenReturn(null)
        // Simulate successful unlock
        `when`(mockAchievementDao.unlockAchievement(any(UserAchievement::class.java)))
            .thenReturn(1L) // Return row ID > 0 for success
        `when`(mockAchievementDao.getAchievementById(AchievementChecker.FIRST_RUN_ID))
            .thenReturn(firstRunAchievement)

        achievementChecker.checkAchievements(userId, newActivityLog)

        verify(mockAchievementDao).unlockAchievement(any(UserAchievement::class.java))
        verify(mockXPCalculator).addXPForAchievement(
            eq(userId),
            eq(firstRunAchievement),
            anyOrNull(), // displayName from Firebase
            anyOrNull()  // email from Firebase
        )
        // Verify Toast was shown (conceptual verification via mockk)
        // This verifies Toast.makeText was called, not that it physically appeared.
        verify { Toast.makeText(mockContext, "Achievement Unlocked: First Run!", Toast.LENGTH_LONG).show() }
    }

    @Test
    fun `checkAchievements FIRST_RUN already unlocked`() = runTest(testDispatcher) {
        val userId = "testUserId"
        val newActivityLog = ActivityLog(
            id = 1L, type = AchievementChecker.RUNNING_GPS_TYPE, timestamp = System.currentTimeMillis(),
            durationMillis = 30 * 60000, caloriesBurned = 300, pathPoints = listOf("0,0","1,1")
        )
        val existingUserAchievement = UserAchievement(userId = userId, achievementId = AchievementChecker.FIRST_RUN_ID, unlockedTimestamp = System.currentTimeMillis())

        // Simulate achievement already unlocked
        `when`(mockAchievementDao.getSpecificUserAchievement(userId, AchievementChecker.FIRST_RUN_ID))
            .thenReturn(existingUserAchievement)

        achievementChecker.checkAchievements(userId, newActivityLog)

        verify(mockAchievementDao, never()).unlockAchievement(any(UserAchievement::class.java))
        verify(mockXPCalculator, never()).addXPForAchievement(anyString(), any(Achievement::class.java), anyOrNull(), anyOrNull())
    }
    
    @Test
    fun `checkAchievements FIRST_RUN wrong activity type`() = runTest(testDispatcher) {
        val userId = "testUserId"
        val newActivityLog = ActivityLog(
            id = 1L, type = "Walking", timestamp = System.currentTimeMillis(), // Not "Running (GPS)"
            durationMillis = 30 * 60000, caloriesBurned = 300, pathPoints = emptyList()
        )

        achievementChecker.checkAchievements(userId, newActivityLog)

        verify(mockAchievementDao, never()).getSpecificUserAchievement(anyString(), anyString())
        verify(mockAchievementDao, never()).unlockAchievement(any(UserAchievement::class.java))
        verify(mockXPCalculator, never()).addXPForAchievement(anyString(), any(Achievement::class.java), anyOrNull(), anyOrNull())
    }
}
