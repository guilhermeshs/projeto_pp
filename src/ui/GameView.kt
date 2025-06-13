package ui


import javafx.scene.control.TextInputDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import db.RankingEntry
import db.DatabaseManager
import controller.GameController
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import controller.GameTimer
import controller.SoundManager
import model.*
import util.createMuteButton
import util.createStyledScene


class GameView(
    private val controller: GameController,
    private val stage: Stage,
    private var gameEnded: Boolean = false
) : BorderPane() {

    private val buttons = mutableMapOf<Card, Button>()
    private var isProcessing = false

    private val turnLabel = Label()
    private val scoreLabel = Label()
    private val timeLabel = Label()
    private val abandonButton = Button("Abandonar Partida")
    private val hintButton = Button("Pedir Dica")

    private val hintLegendLabel = Label("") // << NOVO

    private var gameTimer: GameTimer? = null

    init {
        SoundManager.stop()

        val musicOptions = when (controller.mode) {
            GameMode.COMPETITIVE -> listOf("/sounds/comp1.mp3", "/sounds/comp2.mp3")
            GameMode.COOPERATIVE -> listOf("/sounds/coop1.mp3", "/sounds/coop2.mp3")
            GameMode.ZEN -> listOf("/sounds/zen.mp3")
        }

        if (controller.mode != GameMode.COOPERATIVE) {
            timeLabel.isVisible = false
        }

        val selected = musicOptions.random()
        SoundManager.play(selected)

        val muteButton = createMuteButton()
        hintButton.setOnAction {
            if (controller.hintManager.useHint()) {
                updateView()
            }
        }

        val topPanel = VBox(10.0, turnLabel, scoreLabel, timeLabel, abandonButton, hintButton, hintLegendLabel, muteButton) // << MODIFICADO
        topPanel.padding = Insets(10.0)
        top = topPanel

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0)
        }

        val cols = 6
        controller.cards.forEachIndexed { index, card ->
            val button = Button().apply {
                minWidth = 80.0
                minHeight = 80.0
                setOnAction {
                    if (isProcessing || controller.isMachineTurn()) return@setOnAction
                    if (controller.revealCard(card)) {
                        updateView()
                        if (controller.shouldHideCards()) {
                            handlePostTurn()
                        }
                    }
                }
            }

            buttons[card] = button
            val row = index / cols
            val col = index % cols
            grid.add(button, col, row)
        }

        abandonButton.setOnAction {
            val confirm = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Confirmar Abandono"
                headerText = "Tem certeza que deseja abandonar a partida?"
                contentText = "Seu progresso será perdido."
            }

            val result = confirm.showAndWait()
            if (result.isPresent && result.get().buttonData.isDefaultButton) {
                gameTimer?.stop()
                val menu = MenuView(stage)
                stage.scene = createStyledScene(menu, 1280.0, 720.0)
                scene.stylesheets.add(javaClass.getResource("/style.css")!!.toExternalForm())
            }
        }

        center = grid
        updateLabels()
        updateView()

        if (controller.mode == GameMode.COOPERATIVE) {
            gameTimer = GameTimer(150, timeLabel) {
                showTimeOverAlert()
            }
            gameTimer?.start()
        }
    }

    private fun showTimeOverAlert() {
        val alert = Alert(Alert.AlertType.INFORMATION).apply {
            title = "Tempo Esgotado"
            headerText = "Você perdeu! 😢"
            contentText = "O tempo acabou antes de revelar todas as cartas."
        }
        alert.showAndWait()

        val menu = MenuView(stage)
        stage.scene = createStyledScene(menu, 1280.0, 720.0)
    }

    private fun handlePostTurn() {
        isProcessing = true
        Thread {
            Thread.sleep(1000)
            Platform.runLater {
                controller.hideUnmatched()
                updateView()
                isProcessing = false

                if (controller.mode == GameMode.COOPERATIVE && controller.currentPlayer == PlayerType.MACHINE) {
                    handleMachineTurn()
                } else if (controller.isMachineTurn()) {
                    handleMachineTurn()
                }
            }
        }.start()
    }

    private fun handleMachineTurn() {
        isProcessing = true
        Thread {
            Thread.sleep(1000)
            controller.playMachineTurn()
            Platform.runLater {
                updateView()
            }

            Thread.sleep(1000)
            Platform.runLater {
                controller.hideUnmatched()
                updateView()
                isProcessing = false
            }
        }.start()
    }

    private fun updateView() {


        val reveladoraAtiva = controller.cards.find {
            it.specialType == SpecialType.REVELADORA && it.isRevealed && controller.currentPlayer == PlayerType.HUMAN
        }
        val simboloRevelado = reveladoraAtiva?.symbol
        buttons.forEach { (card, button) ->
            button.styleClass.setAll("card-button")
            if (card.isMatched || card.isRevealed) {
                val image = loadImageForSymbol(card.symbol)
                val imageView = ImageView(image).apply {
                    fitWidth = 60.0
                    fitHeight = 60.0
                    isPreserveRatio = true
                }
                button.graphic = imageView
                button.text = ""

            } else {
                button.graphic = null
                button.text = "❓"
            }

            if (simboloRevelado != null && card.symbol == simboloRevelado && !card.isMatched) {
                button.styleClass.add("card-reveladora")
            }


            // Borda de jogador (matched)
            when (card.isMatchedBy) {
                PlayerType.HUMAN -> {
                    if (controller.mode == GameMode.COOPERATIVE)
                        button.styleClass.add("human-coop-border")
                    else
                        button.styleClass.add("human-border")
                }
                PlayerType.MACHINE -> {
                    if (controller.mode == GameMode.COOPERATIVE)
                        button.styleClass.add("machine-coop-border")
                    else
                        button.styleClass.add("machine-border")
                }
                else -> {}
            }

            // Aplicar estilo da dica (borda dourada)
            if (controller.hintManager.isHinted(card)) {
                button.styleClass.add("hint-border")
            }

            // Aplicar borda de carta especial
            if ((card.isRevealed && !card.isMatched)) { //&& controller.currentPlayer == PlayerType.HUMAN)
                when (card.specialType) {
                    SpecialType.REVELADORA -> button.styleClass.add("card-reveladora")
                    SpecialType.CONGELANTE -> button.styleClass.add("card-congelante")
                    SpecialType.ARMADILHA -> button.styleClass.add("card-armadilha")
                    else -> {}
                }
            }

            // Desabilita botão se carta estiver combinada
            button.isDisable = card.isMatched
        }

        // Atualiza legenda das dicas
        val allHints = controller.hintManager.getAllHintedCards()
        if (allHints.isNotEmpty()) {
            val fruitNames = allHints.mapNotNull { card ->
                controller.hintManager.getHintedSymbol(card)?.let { symbol ->
                    controller.hintManager.symbolToFruit[symbol]
                }
            }
            hintLegendLabel.text = "💡 Cartas douradas: ${fruitNames.joinToString(", ")}"
        } else {
            hintLegendLabel.text = ""
        }

        updateLabels()
        checkGameEnd()
    }


    private fun loadImageForSymbol(symbol: String): Image {
        val path = "/images/${symbol.lowercase()}.png"
        val stream = javaClass.getResourceAsStream(path)
        return if (stream != null) {
            Image(stream)
        } else {
            println("⚠️ Imagem não encontrada: $path")
            Image(javaClass.getResourceAsStream("/imagens/default.png"))
        }
    }

    private fun updateLabels() {
        if (controller.mode == GameMode.ZEN) {
            turnLabel.text = "Modo Zen - Aproveite o jogo!"
            scoreLabel.text = "Cartas reveladas: ${controller.humanScore}"
        } else {
            turnLabel.text = "Vez do Jogador: ${if (controller.currentPlayer == PlayerType.HUMAN) "Humano" else "Máquina"}"
            scoreLabel.text = "Pontuação - Humano: ${controller.humanScore}  |  Máquina: ${controller.machineScore}"
        }
    }

    private fun checkGameEnd() {
        if (gameEnded || !controller.isGameOver()) return

        gameEnded = true
        gameTimer?.stop()

        val winner = when (controller.mode) {
            GameMode.COMPETITIVE -> when {
                controller.humanScore > controller.machineScore -> "Você venceu! 🎉"
                controller.machineScore > controller.humanScore -> "A máquina venceu! 🤖"
                else -> "Empate! 😐"
            }
            GameMode.COOPERATIVE -> "Parabéns! Vocês venceram juntos! 🎉"
            GameMode.ZEN -> "Parabéns!"
        }

        val alert = Alert(Alert.AlertType.INFORMATION).apply {
            title = "Fim de Jogo"
            headerText = winner
            contentText = "Placar final:\nHumano: ${controller.humanScore}  |  Máquina: ${controller.machineScore}"
        }

        alert.showAndWait()

        if(controller.mode != GameMode.ZEN){
            saveRanking()
        }

        val menu = MenuView(stage)
        stage.scene = createStyledScene(menu, 1280.0, 720.0)
    }

    private fun saveRanking() {
        val nameDialog = TextInputDialog("Jogador").apply {
            title = "Salvar Ranking"
            headerText = "Digite seu nome para salvar no ranking"
            contentText = "Nome:"
        }

        val result = nameDialog.showAndWait()
        if (result.isEmpty) return

        val playerName = result.get().take(20) // Limitar tamanho

        val gameResult = when (controller.mode) {
            GameMode.COMPETITIVE -> when {
                controller.humanScore > controller.machineScore -> "Vitória"
                controller.machineScore > controller.humanScore -> "Derrota"
                else -> "Empate"
            }
            GameMode.COOPERATIVE -> "Vitória"
            GameMode.ZEN -> "Completo"
        }

        val rankingEntry = RankingEntry(
            playerName = playerName,
            score = controller.humanScore,
            mode = controller.mode,
            difficulty = controller.difficulty,
            date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            result = gameResult
        )

        DatabaseManager.insertRanking(rankingEntry)
    }

}
