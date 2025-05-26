package com.example.fitnesstrackerapp.challenge.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_challenges",
    foreignKeys = [ForeignKey(
        entity = Challenge::class,
        parentColumns = ["id"],
        childColumns = ["challengeId"],
        onDelete = ForeignKey.CASCADE // If a Challenge is deleted, associated UserChallenge entries are also deleted
    )],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["challengeId"]),
        Index(value = ["userId", "challengeId", "endDate"]) // For querying active challenges
    ]
)
data class UserChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,

    val challengeId: String, // Foreign key to Challenge.id

    val startDate: Long, // Timestamp when the user joined/started this instance of the challenge
    val endDate: Long,   // Timestamp when this instance of the challenge expires

    var currentProgress: Double = 0.0,
    var isCompleted: Boolean = false,
    var isRewardClaimed: Boolean = false
)
