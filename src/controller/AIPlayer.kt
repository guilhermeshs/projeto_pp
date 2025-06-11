package controller

import model.Card
import model.Difficulty
import model.GameMode
import kotlin.random.Random

class AIPlayer(
    difficulty: Difficulty,
    gameMode: GameMode
) {

    private val memorySize = when (difficulty) {
        Difficulty.EASY -> if (gameMode == GameMode.COOPERATIVE) 6 else 3
        Difficulty.MEDIUM -> if (gameMode == GameMode.COOPERATIVE) 12 else 9
        Difficulty.HARD -> Int.MAX_VALUE
        Difficulty.EXTREME -> Int.MAX_VALUE
    }


    private val memory = mutableListOf<Card>()

    fun observe(card: Card) {
        if (memory.any { it.id == card.id }) return
        memory.add(card)
        if (memory.size > memorySize) memory.removeFirst()
    }

    fun chooseCards(board: List<Card>, groupSize: Int): List<Card> {
        val grouped = memory.groupBy { it.symbol }

        // Verifica se a IA conhece um grupo completo
        val match = grouped.values.firstOrNull { group ->
            group.size >= groupSize && group.all { !it.isMatched && !it.isRevealed }
        }

        return if (match != null) {
            // IA conhece todas as cartas de um grupo → joga
            match.take(groupSize)
        } else {
            // IA não conhece grupo completo → vira apenas cartas que ela não conhece ainda
            val unknownCards = board.filter { card ->
                !card.isMatched &&
                        !card.isRevealed &&
                        memory.none { it.id == card.id }
            }

            // fallback: se não houver suficientes desconhecidas, completa aleatoriamente
            if (unknownCards.size >= groupSize) {
                unknownCards.shuffled().take(groupSize)
            } else {
                // fallback seguro: evita cartas reveladas e já combinadas
                board.filter { !it.isMatched && !it.isRevealed }
                    .shuffled()
                    .take(groupSize)
            }
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

    /*fun chooseCards(board: List<Card>, groupSize: Int): List<Card> {
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
    }*/


}
