package com.example.fitnesstrackerapp.challenge.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChallengeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<Challenge>)

    @Query("SELECT * FROM challenges WHERE isActiveGlobally = 1")
    fun getGlobalActiveChallenges(): LiveData<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun joinChallenge(userChallenge: UserChallenge): Long // Returns row ID or -1 if ignored

    @Query("SELECT * FROM user_challenges WHERE userId = :userId ORDER BY endDate DESC")
    fun getUserChallenges(userId: String): LiveData<List<UserChallenge>>

    @Query("SELECT * FROM user_challenges WHERE userId = :userId AND challengeId = :challengeId AND endDate > :currentTime")
    suspend fun getActiveUserChallengeInstance(userId: String, challengeId: String, currentTime: Long): UserChallenge?

    @Query("SELECT * FROM user_challenges WHERE userId = :userId AND isCompleted = 0 AND endDate > :currentTime")
    suspend fun getActiveIncompleteUserChallenges(userId: String, currentTime: Long): List<UserChallenge>

    @Query("UPDATE user_challenges SET currentProgress = :progress, isCompleted = :completed WHERE id = :userChallengeId")
    suspend fun updateUserChallengeProgress(userChallengeId: Long, progress: Double, completed: Boolean)

    @Query("UPDATE user_challenges SET isRewardClaimed = 1 WHERE id = :userChallengeId")
    suspend fun claimReward(userChallengeId: Long)

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    suspend fun getChallengeById(challengeId: String): Challenge? // Added

    @Query("SELECT * FROM user_challenges WHERE id = :userChallengeId")
    suspend fun getUserChallengeById(userChallengeId: Long): UserChallenge? // Added
}
