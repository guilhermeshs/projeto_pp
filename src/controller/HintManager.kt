package controller

import model.Card
import model.Difficulty
import javafx.scene.control.Alert
import kotlin.random.Random
import model.SpecialType

class HintManager(private val controller: GameController, difficulty: Difficulty) {

    val symbolToFruit = mapOf(
        "A" to "maçã",
        "B" to "banana",
        "C" to "laranja",
        "D" to "morango",
        "E" to "melancia",
        "F" to "laranjeira",
        "G" to "tigela",
        "H" to "uva",
        "I" to "limão",
        "J" to "manga",
        "K" to "pera",
        "L" to "abacaxi"
    )

    private val motivationalPhrases = listOf(
        "Continue tentando, você está indo bem!",
        "A memória melhora com prática!",
        "Você consegue!",
        "Respire fundo e observe com atenção!"
    )

    private var remainingHints = when (difficulty) {
        Difficulty.EASY -> 3
        Difficulty.MEDIUM -> 2
        Difficulty.HARD -> 1
        Difficulty.EXTREME -> 0
    }

    val revealedHints = mutableMapOf<Card, String>()  // Carta e o símbolo revelado

    fun getAllHintedCards(): List<Card> {
        return revealedHints.keys.toList()
    }


    fun useHint(): Boolean {
        if (remainingHints <= 0) {
            showAlert("Sem Dicas", "Você não tem mais dicas disponíveis.")
            return false
        }

        remainingHints--

        val randomChance = Random.nextInt(100)
        if (randomChance < 25) {
            val phrase = motivationalPhrases.random()
            showAlert("Dica", phrase)
            return true
        }

        val candidates = controller.cards.filter {
            !it.isMatched && !it.isRevealed && !revealedHints.containsKey(it)
        }

        if (candidates.isEmpty()) {
            showAlert("Dica", "Todas as cartas já foram reveladas ou combinadas.")
            return true
        }

        val chosen = candidates.random()
        revealedHints[chosen] = chosen.symbol

        val fruitName = symbolToFruit[chosen.symbol] ?: "fruta desconhecida"
        showAlert("Dica", "Uma carta foi destacada! Ela representa: $fruitName.")
        return true
    }
    fun isHinted(card: Card): Boolean {
        return revealedHints.containsKey(card)
    }

    fun getHintedCard(): Card? {
        return revealedHints.keys.lastOrNull()
    }

    fun getHintedSymbol(card: Card): String? {
        return revealedHints[card]
    }

    private fun showAlert(title: String, message: String) {
        Alert(Alert.AlertType.INFORMATION).apply {
            this.title = title
            headerText = null
            contentText = message
            showAndWait()
        }
    }

    fun getRemainingHints(): Int = remainingHints
}
