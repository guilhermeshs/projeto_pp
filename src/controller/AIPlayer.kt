package controller

import model.Card
import model.Difficulty
import kotlin.random.Random

class AIPlayer(difficulty: Difficulty) {

    private val memorySize = when (difficulty) {
        Difficulty.EASY -> 3     // 1 jogada = 2 cartas
        Difficulty.MEDIUM -> 6   // 3 jogadas = 6 cartas
        Difficulty.HARD -> Int.MAX_VALUE // SUPER INTELIGENTE.
        Difficulty.EXTREME -> 8  // opcional: IA mais caótica
    }

    private val memory = mutableListOf<Card>()

    fun observe(card: Card) {
        if (memory.any { it.id == card.id }) return
        memory.add(card)
        if (memory.size > memorySize) memory.removeFirst()
    }

    fun chooseCards(board: List<Card>, groupSize: Int): List<Card> {
        val grouped = memory.groupBy { it.symbol }
        val match = grouped.values.firstOrNull { group ->
            group.size >= groupSize && group.all { !it.isMatched && !it.isRevealed }
        }

        return if (match != null) {
            match.take(groupSize)
        } else {
            // fallback aleatório
            board.filter { !it.isMatched && !it.isRevealed }
                .shuffled()
                .take(groupSize)
        }
    }

    fun printMemory() {
        if (memory.isEmpty()) {
            println("[AI DEBUG] Memória vazia.")
            return
        }

        val symbolToFruit = mapOf(
            "A" to "maçã",
            "B" to "banana",
            "C" to "laranja",
            "D" to "morango",
            "E" to "melancia",
            "F" to "laranjeira",
            "G" to "tigeja",
            "H" to "uva",
            "I" to "limão",
            "J" to "manga",
            "K" to "pera",
            "L" to "abacaxi"
        )

        println("[AI DEBUG] Memória atual da IA:")
        memory.groupBy { it.symbol }.forEach { (symbol, cards) ->
            val fruta = symbolToFruit[symbol.uppercase()] ?: symbol
            val ids = cards.joinToString { it.id.toString() }
            println("  Fruta '$fruta' -> Cartas com IDs: $ids")
        }
    }


}
