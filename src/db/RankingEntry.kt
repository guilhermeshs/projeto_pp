package db

import model.GameMode
import model.Difficulty

data class RankingEntry(
    val playerName: String,
    val score: Int,
    val mode: GameMode,
    val difficulty: Difficulty,
    val date: String,
    val result: String
)
