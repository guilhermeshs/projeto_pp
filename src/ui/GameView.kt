package ui

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
import model.Card
import model.GameMode
import model.PlayerType
import controller.GameTimer
import controller.SoundManager
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
        val topPanel = VBox(10.0, turnLabel, scoreLabel, timeLabel, abandonButton, muteButton)
        topPanel.padding = Insets(10.0)
        top = topPanel


        if (controller.mode == GameMode.COMPETITIVE) {
            timeLabel.isVisible = false
        }

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
            gameTimer = GameTimer(300, timeLabel) {
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
        buttons.forEach { (card, button) ->
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

            button.styleClass.setAll("card-button")

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

            button.isDisable = card.isMatched
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

        val menu = MenuView(stage)
        stage.scene = createStyledScene(menu, 1280.0, 720.0)
    }

}
