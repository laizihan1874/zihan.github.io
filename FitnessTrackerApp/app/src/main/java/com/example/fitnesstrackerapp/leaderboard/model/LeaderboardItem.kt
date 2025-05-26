package com.example.fitnesstrackerapp.leaderboard.model

data class LeaderboardItem(
    val rank: Int,
    val userId: String,
    val displayName: String?,
    val xpPoints: Long,
    val isCurrentUser: Boolean
)
