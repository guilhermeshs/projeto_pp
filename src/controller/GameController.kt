package controller

import model.Card
import model.PlayerType
import kotlin.random.Random

class GameController(cardCount: Int) {

    val cards: List<Card>
    private val selectedCards = mutableListOf<Card>()
    private val memory = mutableMapOf<String, Int>() // simbolo -> posição (memória da IA)

    var currentPlayer = PlayerType.HUMAN
        private set

    val humanScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.HUMAN } / 2

    val machineScore: Int
        get() = cards.count { it.isMatchedBy == PlayerType.MACHINE } / 2


    init {
        val symbols = ('A'..'Z').take(cardCount / 2).flatMap { listOf(it.toString(), it.toString()) }
        cards = symbols.shuffled().mapIndexed { index, symbol ->
            Card(index, symbol)
        }
    }

    fun revealCard(card: Card): Boolean {
        if (card.isMatched || card.isRevealed || selectedCards.size >= 2) return false

        card.isRevealed = true
        selectedCards.add(card)

        // IA aprende a carta revelada
        if (currentPlayer == PlayerType.MACHINE) {
            memory[card.symbol] = card.id
        }

        return true
    }

    fun shouldHideCards(): Boolean = selectedCards.size == 2

    fun hideUnmatched() {
        if (selectedCards.size != 2) return

        val (first, second) = selectedCards
        if (first.symbol == second.symbol) {
            first.isMatched = true
            second.isMatched = true
            first.isMatchedBy = currentPlayer
            second.isMatchedBy = currentPlayer
        }
        else {
            first.isRevealed = false
            second.isRevealed = false
        }
        selectedCards.clear()
        switchTurn()
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == PlayerType.HUMAN) PlayerType.MACHINE else PlayerType.HUMAN
    }

    fun isMachineTurn(): Boolean = currentPlayer == PlayerType.MACHINE

    fun playMachineTurn(): List<Card> {
        val unrevealed = cards.filter { !it.isRevealed && !it.isMatched }

        // 👇 estratégia de memória simples: se souber onde tem par, joga isso
        val knownPairs = memory.entries.groupBy({ it.key }, { it.value })
            .filter { it.value.size >= 2 }

        val selected = when {
            knownPairs.isNotEmpty() -> {
                // 👀 joga um par que lembrou
                val pairPositions = knownPairs.entries.first().value.take(2)
                pairPositions.mapNotNull { id -> cards.find { it.id == id } }
            }

            else -> {
                // 🎲 estratégia fallback: escolhe dois aleatórios
                unrevealed.shuffled().take(2)
            }
        }

        selected.forEach { revealCard(it) }
        return selected
    }

    fun isGameOver(): Boolean {
        return cards.all { it.isMatched }
    }

}
