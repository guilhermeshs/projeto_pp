package controller

import model.Card
import model.Difficulty
import model.GameMode
import model.PlayerType
import kotlin.random.Random

class GameController(
    val mode: GameMode,
    private val difficulty: Difficulty
) {
    val cards: List<Card>
    private val selectedCards = mutableListOf<Card>()
    private val aiPlayer = AIPlayer(difficulty)


    var currentPlayer = PlayerType.HUMAN
        private set

    val humanScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.HUMAN } / groupSize()

    val machineScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.MACHINE } / groupSize()

    init {
        val symbols = when (difficulty) {
            Difficulty.EASY -> generateGroups(12, 2)      // 12 pares
            Difficulty.MEDIUM -> generateGroups(8, 3)     // 8 trincas
            Difficulty.HARD -> generateGroups(6, 4)       // 6 quadras
            Difficulty.EXTREME -> generateExtremeGroups() // Mistura
        }

        cards = symbols.shuffled().mapIndexed { index, symbol -> Card(index, symbol) }
    }

    private fun groupSize(): Int = when (difficulty) {
        Difficulty.EASY -> 2
        Difficulty.MEDIUM -> 3
        Difficulty.HARD -> 4
        Difficulty.EXTREME -> 1 // para não dividir, já que os grupos são mistos
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
        val groups = listOf(2, 3, 3, 4, 4, 2) // soma 24
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

        if (mode != GameMode.ZEN) {
            aiPlayer.observe(card)
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
        val groupSize = groupSize()
        val chosen = aiPlayer.chooseCards(cards, groupSize)
        chosen.forEach { revealCard(it) }

        aiPlayer.printMemory()

        return chosen
    }

    fun isGameOver(): Boolean = cards.all { it.isMatched }
}