package controller

import model.Card
import model.Difficulty
import model.GameMode
import model.PlayerType

class GameController(
    val mode: GameMode,
    private val difficulty: Difficulty
) {
    val cards: List<Card>
    private val selectedCards = mutableListOf<Card>()
    private val memory = mutableMapOf<String, Int>()

    var currentPlayer = PlayerType.HUMAN
        private set

    val humanScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.HUMAN } / groupSize()

    val machineScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.MACHINE } / groupSize()

    init {
        val symbols = when (difficulty) {
            Difficulty.EASY -> generateGroups(12, 2)        // 24 cartas
            Difficulty.MEDIUM -> generateGroups(8, 3)       // 24 cartas
            Difficulty.HARD -> generateGroups(6, 4)         // 24 cartas
            Difficulty.EXTREME -> generateExtremeGroups()   // também 24 cartas
        }

        cards = symbols.shuffled().mapIndexed { index, symbol -> Card(index, symbol) }
    }

    private fun groupSize(): Int = when (difficulty) {
        Difficulty.EASY -> 2
        Difficulty.MEDIUM -> 3
        Difficulty.HARD, Difficulty.EXTREME -> 4
    }

    private fun generateGroups(groupCount: Int, groupSize: Int): List<String> {
        val availableSymbols = ('A'..'Z').iterator()
        val list = mutableListOf<String>()
        repeat(groupCount) {
            val symbol = availableSymbols.next().toString()
            repeat(groupSize) {
                list.add(symbol)
            }
        }
        return list
    }

    private fun generateExtremeGroups(): List<String> {
        val availableSymbols = ('A'..'Z').iterator()
        // Nova distribuição: 2 + 2 + 3 + 3 + 4 + 4 + 3 + 3 = 24
        val groups = listOf(2, 2, 3, 3, 4, 4, 3, 3)
        val list = mutableListOf<String>()

        for (groupSize in groups) {
            val symbol = availableSymbols.next().toString()
            repeat(groupSize) {
                list.add(symbol)
            }
        }

        return list
    }

    fun revealCard(card: Card): Boolean {
        if (card.isMatched || card.isRevealed || selectedCards.size >= groupSize()) return false

        card.isRevealed = true
        selectedCards.add(card)

        if (currentPlayer == PlayerType.MACHINE) {
            memory[card.symbol] = card.id
        }

        return true
    }

    fun shouldHideCards(): Boolean = selectedCards.size == groupSize()

    fun hideUnmatched() {
        if (selectedCards.size != groupSize()) return

        val allSame = selectedCards.all { it.symbol == selectedCards.first().symbol }

        if (allSame) {
            selectedCards.forEach {
                it.isMatched = true
                it.isMatchedBy = currentPlayer
            }
        } else {
            selectedCards.forEach { it.isRevealed = false }
        }

        selectedCards.clear()
        switchTurn()
    }

    private fun switchTurn() {
        if (mode == GameMode.ZEN) return
        currentPlayer = if (currentPlayer == PlayerType.HUMAN) PlayerType.MACHINE else PlayerType.HUMAN
    }

    fun isMachineTurn(): Boolean = mode == GameMode.COMPETITIVE && currentPlayer == PlayerType.MACHINE

    fun playMachineTurn(): List<Card> {
        val unrevealed = cards.filter { !it.isRevealed && !it.isMatched }

        val knownGroups = memory.entries.groupBy({ it.key }, { it.value })
            .filter { it.value.size >= groupSize() }

        val selected = when {
            knownGroups.isNotEmpty() -> {
                val groupPositions = knownGroups.entries.first().value.take(groupSize())
                groupPositions.mapNotNull { id -> cards.find { it.id == id } }
            }
            else -> unrevealed.shuffled().take(groupSize())
        }

        selected.forEach { revealCard(it) }
        return selected
    }

    fun isGameOver(): Boolean = cards.all { it.isMatched }
}
