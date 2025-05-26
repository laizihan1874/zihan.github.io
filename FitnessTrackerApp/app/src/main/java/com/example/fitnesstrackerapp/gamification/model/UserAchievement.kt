package com.example.fitnesstrackerapp.gamification.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_achievements",
    foreignKeys = [ForeignKey(
        entity = Achievement::class,
        parentColumns = ["id"],
        childColumns = ["achievementId"],
        onDelete = ForeignKey.CASCADE // Optional: Define action on Achievement deletion
    )]
)
data class UserAchievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(index = true)
    val userId: String, // Firebase UID of the user

    @ColumnInfo(index = true)
    val achievementId: String, // Foreign key to Achievement.id

    val unlockedTimestamp: Long
)
