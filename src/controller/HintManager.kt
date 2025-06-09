package controller

import model.Card
import model.Difficulty
import javafx.scene.control.Alert
import kotlin.random.Random

class HintManager(private val controller: GameController, difficulty: Difficulty) {

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

        showAlert("Dica", "Uma carta foi revelada temporariamente!")
        return true
    }

    fun isHinted(card: Card): Boolean {
        return revealedHints.containsKey(card)
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
