package com.example.fitnesstrackerapp.user.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(userProfile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): LiveData<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfileSuspending(userId: String): UserProfile?

    @Query("UPDATE user_profiles SET xpPoints = :xp, level = :level WHERE userId = :userId")
    suspend fun updateUserXPAndLevel(userId: String, xp: Long, level: Int)
}
