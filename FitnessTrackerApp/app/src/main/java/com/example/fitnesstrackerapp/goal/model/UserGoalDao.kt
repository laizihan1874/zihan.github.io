package com.example.fitnesstrackerapp.goal.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserGoal(goal: UserGoal)

    @Query("SELECT * FROM user_goals WHERE userId = :userId ORDER BY isActive DESC, isCompleted ASC, targetDate ASC, startDate DESC")
    fun getAllUserGoals(userId: String): LiveData<List<UserGoal>>

    @Query("SELECT * FROM user_goals WHERE userId = :userId AND isActive = 1 AND isCompleted = 0")
    suspend fun getActiveUserGoals(userId: String): List<UserGoal>

    @Query("UPDATE user_goals SET currentValue = :currentProgress, lastUpdated = :timestamp, isCompleted = :completed WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: Long, currentProgress: Double, timestamp: Long, completed: Boolean)

    @Query("DELETE FROM user_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)
}
