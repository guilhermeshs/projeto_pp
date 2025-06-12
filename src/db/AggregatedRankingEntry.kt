package db

import model.Difficulty

data class AggregatedRankingEntry(
    val playerName: String,
    val totalScore: Int,
    val victories: Int,
    val defeats: Int,
    val averageScore: Double,
    val lastDifficulty: Difficulty,
    val lastDate: String
)
