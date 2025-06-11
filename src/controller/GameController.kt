package controller

import model.Card
import model.Difficulty
import model.GameMode
import model.PlayerType
import kotlin.random.Random
import model.SpecialType

class GameController(
    val mode: GameMode,
    private val difficulty: Difficulty
) {
    var iaCongelada = false
    var iaDesativada = false
    val cards: List<Card>
    private val selectedCards = mutableListOf<Card>()
    private val aiPlayer = AIPlayer(difficulty)
    lateinit var hintManager: HintManager

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

        val tempCards = symbols.shuffled().mapIndexed { index, symbol -> Card(index, symbol) }

        assignSpecialCards(tempCards)

        cards = tempCards
        hintManager = HintManager(this, difficulty)
    }

    private fun assignSpecialCards(cards: List<Card>) {
        val rng = Random(System.currentTimeMillis())

        when (mode) {
            GameMode.COMPETITIVE -> {
                val reveladoraCount = 1
                val congelanteCount = 2

                cards.filter { it.specialType == SpecialType.NONE }
                    .shuffled(rng)
                    .take(reveladoraCount + congelanteCount)
                    .forEachIndexed { i, card ->
                        card.specialType = if (i < reveladoraCount)
                            SpecialType.REVELADORA
                        else
                            SpecialType.CONGELANTE
                    }
            }

            GameMode.COOPERATIVE -> {
                val armadilhaCount = 2

                cards.filter { it.specialType == SpecialType.NONE }
                    .shuffled(rng)
                    .take(armadilhaCount)
                    .forEach { it.specialType = SpecialType.ARMADILHA }
            }

            GameMode.ZEN -> {
                // Nenhuma carta especial
            }
        }
    }


    private fun groupSize(): Int = when (difficulty) {
        Difficulty.EASY -> 2
        Difficulty.MEDIUM -> 3
        Difficulty.HARD -> 4
        Difficulty.EXTREME -> 1
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
        // Efeito: Carta Reveladora (somente se jogador for humano)
        if (currentPlayer == PlayerType.HUMAN &&
            card.specialType == SpecialType.REVELADORA &&
            mode == GameMode.COMPETITIVE
        ) {
            // Destacar todas as cartas com o mesmo símbolo (efeito visual via GameView)
            highlightMatchingCards(card.symbol)
        }

        return true
    }


    fun highlightMatchingCards(symbol: String): List<Card> {
        return cards.filter { it.symbol == symbol && !it.isMatched }
    }


    fun shouldHideCards(): Boolean = selectedCards.size == groupSize()

    fun hideUnmatched() {
        if (selectedCards.size != groupSize()) return

        val allSame = selectedCards.all { it.symbol == selectedCards.first().symbol }
        val hasCongelante = selectedCards.any { it.specialType == SpecialType.CONGELANTE }
        val hasArmadilha = selectedCards.any { it.specialType == SpecialType.ARMADILHA }

        if (allSame) {
            selectedCards.forEach {
                it.isMatched = true
                it.isMatchedBy = currentPlayer
            }

            // Efeito: Congelante (somente se jogador for humano)
            if (currentPlayer == PlayerType.HUMAN && hasCongelante && mode == GameMode.COMPETITIVE) {
                iaCongelada = true
            }
        } else {
            // Efeito: Armadilha (somente se jogador for humano)
            if (currentPlayer == PlayerType.HUMAN && hasArmadilha && mode == GameMode.COOPERATIVE) {
                iaDesativada = true
            }

            selectedCards.forEach { it.isRevealed = false }
        }

        selectedCards.clear()
        switchTurn()
    }


    private fun switchTurn() {
        if (mode == GameMode.ZEN) return

        if(iaDesativada) return

        currentPlayer = if (currentPlayer == PlayerType.HUMAN) PlayerType.MACHINE else PlayerType.HUMAN

        if (currentPlayer == PlayerType.MACHINE && iaCongelada && mode == GameMode.COMPETITIVE) {
            iaCongelada = false
            currentPlayer = PlayerType.HUMAN
        }
    }


    fun isMachineTurn(): Boolean {
        if (mode != GameMode.COMPETITIVE) return false
        if (iaDesativada) return false
        if (iaCongelada) {
            iaCongelada = false
            return false
        }
        return currentPlayer == PlayerType.MACHINE
    }

    fun playMachineTurn(): List<Card> {
        val groupSize = groupSize()
        val chosen = aiPlayer.chooseCards(cards, groupSize)
        chosen.forEach { revealCard(it) }

        aiPlayer.printMemory()

        return chosen
    }

    fun isGameOver(): Boolean = cards.all { it.isMatched }
}