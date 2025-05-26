package com.example.fitnesstrackerapp.gamification.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): LiveData<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockAchievement(userAchievement: UserAchievement): Long // Returns row ID or -1 if ignored

    @Query("SELECT * FROM user_achievements WHERE userId = :userId")
    fun getUnlockedAchievementsForUser(userId: String): LiveData<List<UserAchievement>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun getSpecificUserAchievement(userId: String, achievementId: String): UserAchievement?
}
