package com.example.fitnesstrackerapp.tracking.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(activityLog: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): LiveData<List<ActivityLog>> // Using LiveData for automatic UI updates

    @Query("SELECT * FROM activity_logs WHERE id = :logId")
    suspend fun getActivityLogById(logId: Long): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActivitiesForUser(userId: String, limit: Int): List<ActivityLog>

    // This one is also useful for more flexible date ranges if needed later
    @Query("SELECT * FROM activity_logs WHERE userId = :userId AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getActivitiesSince(userId: String, sinceTimestamp: Long): List<ActivityLog>
}
