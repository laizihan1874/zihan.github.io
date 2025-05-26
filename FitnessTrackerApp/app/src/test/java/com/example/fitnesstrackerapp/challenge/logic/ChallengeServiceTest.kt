package com.example.fitnesstrackerapp.challenge.logic

import android.content.Context
import com.example.fitnesstrackerapp.challenge.model.Challenge
import com.example.fitnesstrackerapp.challenge.model.ChallengeDao
import com.example.fitnesstrackerapp.challenge.model.ChallengeTypes
import com.example.fitnesstrackerapp.challenge.model.UserChallenge
import com.example.fitnesstrackerapp.gamification.logic.AchievementChecker // For RUNNING_GPS_TYPE
import com.example.fitnesstrackerapp.gamification.logic.XPCalculator
import com.example.fitnesstrackerapp.social.repository.Result // Ensure this is the correct Result class
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.user.model.UserProfile
import com.example.fitnesstrackerapp.user.model.UserProfileDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class ChallengeServiceTest {

    @MockK
    private lateinit var mockChallengeDao: ChallengeDao
    @MockK
    private lateinit var mockUserProfileDao: UserProfileDao
    @MockK
    private lateinit var mockXPCalculator: XPCalculator
    @MockK
    private lateinit var mockContext: Context // For ChallengeService constructor

    private lateinit var challengeService: ChallengeService

    private val testUserId = "testUser1"
    private val testUserName = "Test User"
    private val testUserEmail = "test@example.com"
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        mockChallengeDao = mockk(relaxed = true) // relaxed to avoid mocking all DAO returns initially
        mockUserProfileDao = mockk(relaxed = true)
        mockXPCalculator = mockk(relaxed = true) // relaxed for addXP calls
        mockContext = mockk(relaxed = true)

        challengeService = ChallengeService(mockChallengeDao, mockUserProfileDao, mockXPCalculator, mockContext)
    }

    private fun createSampleChallenge(
        id: String = "TEST_CHALLENGE",
        type: String = ChallengeTypes.LOG_ACTIVITY_COUNT,
        target: Double = 1.0,
        durationDays: Int = 1,
        xp: Int = 50,
        activityFilter: String? = null
    ) = Challenge(id, "Test Challenge", "Desc", type, activityFilter, target, durationDays, xp, true)

    private fun createSampleUserChallenge(
        challengeId: String,
        userId: String = testUserId,
        startDate: Long = now - TimeUnit.HOURS.toMillis(1), // Started an hour ago
        endDate: Long = now + ChallengeService.DAY_IN_MS - TimeUnit.HOURS.toMillis(1), // Ends in 23 hours
        currentProgress: Double = 0.0,
        isCompleted: Boolean = false,
        isRewardClaimed: Boolean = false,
        id: Long = 1L
    ) = UserChallenge(id, userId, challengeId, startDate, endDate, currentProgress, isCompleted, isRewardClaimed)

    private fun createActivityLog(
        type: String,
        timestamp: Long = now,
        durationMillis: Long = TimeUnit.MINUTES.toMillis(30),
        pathPoints: List<String> = emptyList()
    ) = ActivityLog(1L, testUserId, type, timestamp, durationMillis, 200, "notes", pathPoints)


    // --- joinChallenge Tests ---
    @Test
    fun `joinChallenge success for new challenge`() = runTest {
        val challenge = createSampleChallenge()
        coEvery { mockChallengeDao.getActiveUserChallengeInstance(testUserId, challenge.id, any()) } returns null
        coEvery { mockChallengeDao.joinChallenge(any()) } returns 1L // Simulate successful insert

        val result = challengeService.joinChallenge(testUserId, challenge)

        assertTrue(result is Result.Success)
        assertEquals(challenge.id, (result as Result.Success).data.challengeId)
        coVerify { mockChallengeDao.joinChallenge(match { it.challengeId == challenge.id && it.userId == testUserId }) }
    }

    @Test
    fun `joinChallenge fails if already active`() = runTest {
        val challenge = createSampleChallenge()
        val existingUserChallenge = createSampleUserChallenge(challenge.id)
        coEvery { mockChallengeDao.getActiveUserChallengeInstance(testUserId, challenge.id, any()) } returns existingUserChallenge

        val result = challengeService.joinChallenge(testUserId, challenge)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message?.contains("Already joined") == true)
        coVerify(exactly = 0) { mockChallengeDao.joinChallenge(any()) }
    }

    // --- updateChallengeProgressOnActivityLogged Tests ---
    @Test
    fun `updateChallengeProgress LOG_ACTIVITY_COUNT success and completes challenge`() = runTest {
        val challenge = createSampleChallenge(id = "LOG_ONE_RUN", type = ChallengeTypes.LOG_ACTIVITY_COUNT, target = 1.0, activityFilter = AchievementChecker.RUNNING_GPS_TYPE)
        val userChallenge = createSampleUserChallenge(challengeId = challenge.id, currentProgress = 0.0)
        val activity = createActivityLog(type = AchievementChecker.RUNNING_GPS_TYPE)
        
        coEvery { mockChallengeDao.getActiveIncompleteUserChallenges(testUserId, any()) } returns listOf(userChallenge)
        coEvery { mockChallengeDao.getChallengeById(challenge.id) } returns challenge // Mocked for getChallengeDetails
        coEvery { mockUserProfileDao.getUserProfileSuspending(testUserId) } returns UserProfile(testUserId, testUserName, testUserEmail) // For claim reward

        challengeService.updateChallengeProgressOnActivityLogged(testUserId, activity)

        val progressSlot = slot<Double>()
        val completedSlot = slot<Boolean>()
        coVerify { mockChallengeDao.updateUserChallengeProgress(eq(userChallenge.id), capture(progressSlot), capture(completedSlot)) }
        assertEquals(1.0, progressSlot.captured, 0.0)
        assertTrue(completedSlot.captured)
        coVerify { mockXPCalculator.addXP(testUserId, challenge.xpReward, testUserName, testUserEmail) }
        coVerify { mockChallengeDao.claimReward(userChallenge.id) }
    }
    
    @Test
    fun `updateChallengeProgress DISTANCE_KM success`() = runTest {
        val challenge = createSampleChallenge(id = "RUN_2KM", type = ChallengeTypes.DISTANCE_KM, target = 2.0, activityFilter = AchievementChecker.RUNNING_GPS_TYPE)
        val userChallenge = createSampleUserChallenge(challengeId = challenge.id, currentProgress = 0.5)
        // Approx 1km path (0.009 degree lat change)
        val activity = createActivityLog(type = AchievementChecker.RUNNING_GPS_TYPE, pathPoints = listOf("0.0,0.0", "0.009,0.0")) 

        coEvery { mockChallengeDao.getActiveIncompleteUserChallenges(testUserId, any()) } returns listOf(userChallenge)
        coEvery { mockChallengeDao.getChallengeById(challenge.id) } returns challenge
        
        challengeService.updateChallengeProgressOnActivityLogged(testUserId, activity)

        val progressSlot = slot<Double>()
        coVerify { mockChallengeDao.updateUserChallengeProgress(eq(userChallenge.id), capture(progressSlot), eq(false)) }
        // Distance calc is approx 1.0km for the path
        assertEquals(0.5 + 1.0, progressSlot.captured, 0.1) 
    }


    @Test
    fun `updateChallengeProgress no update if activityTypeFilter mismatch`() = runTest {
        val challenge = createSampleChallenge(id = "LOG_YOGA", type = ChallengeTypes.LOG_ACTIVITY_COUNT, target = 1.0, activityFilter = "Yoga")
        val userChallenge = createSampleUserChallenge(challengeId = challenge.id)
        val activity = createActivityLog(type = "Running (GPS)") // Mismatched type

        coEvery { mockChallengeDao.getActiveIncompleteUserChallenges(testUserId, any()) } returns listOf(userChallenge)
        coEvery { mockChallengeDao.getChallengeById(challenge.id) } returns challenge

        challengeService.updateChallengeProgressOnActivityLogged(testUserId, activity)

        coVerify(exactly = 0) { mockChallengeDao.updateUserChallengeProgress(any(), any(), any()) }
    }
    
    @Test
    fun `updateChallengeProgress no update if activity outside challenge dates`() = runTest {
        val challenge = createSampleChallenge(id = "ACTIVE_CHALLENGE", type = ChallengeTypes.ACTIVE_MINUTES, target = 30.0)
        // Challenge started now, ends in 1 day
        val userChallenge = createSampleUserChallenge(challengeId = challenge.id, startDate = now, endDate = now + ChallengeService.DAY_IN_MS)
        // Activity logged 2 days ago, outside the challenge window
        val activity = createActivityLog(type = "Walking", timestamp = now - TimeUnit.DAYS.toMillis(2))

        coEvery { mockChallengeDao.getActiveIncompleteUserChallenges(testUserId, any()) } returns listOf(userChallenge)
        coEvery { mockChallengeDao.getChallengeById(challenge.id) } returns challenge

        challengeService.updateChallengeProgressOnActivityLogged(testUserId, activity)
        coVerify(exactly = 0) { mockChallengeDao.updateUserChallengeProgress(any(), any(), any()) }
    }


    // --- claimChallengeReward Tests ---
    @Test
    fun `claimChallengeReward success`() = runTest {
        val challenge = createSampleChallenge(xpReward = 150)
        val userChallenge = createSampleUserChallenge(challenge.id, isCompleted = true, isRewardClaimed = false)
        
        coEvery { mockChallengeDao.getUserChallengeById(userChallenge.id) } returns userChallenge
        coEvery { mockChallengeDao.getChallengeById(challenge.id) } returns challenge // For getting name in message
        coEvery { mockUserProfileDao.getUserProfileSuspending(testUserId) } returns UserProfile(testUserId, testUserName, testUserEmail)

        val result = challengeService.claimChallengeReward(testUserId, userChallenge.id, challenge.xpReward, testUserName, testUserEmail)

        assertTrue(result is Result.Success)
        coVerify { mockXPCalculator.addXP(testUserId, challenge.xpReward, testUserName, testUserEmail) }
        coVerify { mockChallengeDao.claimReward(userChallenge.id) }
    }

    @Test
    fun `claimChallengeReward fails if already claimed`() = runTest {
        val challenge = createSampleChallenge()
        val userChallenge = createSampleUserChallenge(challenge.id, isCompleted = true, isRewardClaimed = true)
        coEvery { mockChallengeDao.getUserChallengeById(userChallenge.id) } returns userChallenge

        val result = challengeService.claimChallengeReward(testUserId, userChallenge.id, challenge.xpReward, testUserName, testUserEmail)

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { mockXPCalculator.addXP(any(), any(), any(), any()) }
        coVerify(exactly = 0) { mockChallengeDao.claimReward(any()) }
    }
    
    @Test
    fun `claimChallengeReward fails if not completed`() = runTest {
        val challenge = createSampleChallenge()
        val userChallenge = createSampleUserChallenge(challenge.id, isCompleted = false, isRewardClaimed = false)
        coEvery { mockChallengeDao.getUserChallengeById(userChallenge.id) } returns userChallenge

        val result = challengeService.claimChallengeReward(testUserId, userChallenge.id, challenge.xpReward, testUserName, testUserEmail)

        assertTrue(result is Result.Error)
    }
}
