package model

data class Card(
    val id: Int,
    val symbol: String,
    var isMatched: Boolean = false,
    var isRevealed: Boolean = false,
    var isMatchedBy: PlayerType? = null

)
