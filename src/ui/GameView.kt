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

class GameView(
    private val controller: GameController,
    private val stage: Stage
) : BorderPane() {

    private val buttons = mutableMapOf<Card, Button>()
    private var isProcessing = false

    private val turnLabel = Label()
    private val scoreLabel = Label()
    private val timeLabel = Label("Tempo restante: 03:00")
    private val abandonButton = Button("Abandonar Partida")

    private var timeLeftInSeconds = 180
    private var timerThread: Thread? = null

    init {
        val topPanel = VBox(10.0, turnLabel, scoreLabel, timeLabel, abandonButton)
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
                val menu = MenuView(stage)
                stage.scene = javafx.scene.Scene(menu, 800.0, 600.0)
            }
        }

        center = grid
        updateLabels()
        updateView()

        if (controller.mode == GameMode.COOPERATIVE) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerThread = Thread {
            while (timeLeftInSeconds > 0 && !controller.isGameOver()) {
                Thread.sleep(1000)
                timeLeftInSeconds--

                Platform.runLater {
                    updateTimeLabel()
                }
            }

            if (!controller.isGameOver()) {
                Platform.runLater {
                    showTimeOverAlert()
                }
            }
        }
        timerThread?.start()
    }

    private fun updateTimeLabel() {
        val minutes = timeLeftInSeconds / 60
        val seconds = timeLeftInSeconds % 60
        timeLabel.text = "Tempo restante: %02d:%02d".format(minutes, seconds)
    }

    private fun showTimeOverAlert() {
        val alert = Alert(Alert.AlertType.INFORMATION).apply {
            title = "Tempo Esgotado"
            headerText = "Você perdeu! 😢"
            contentText = "O tempo acabou antes de revelar todas as cartas."
        }
        alert.showAndWait()

        val menu = MenuView(stage)
        stage.scene = javafx.scene.Scene(menu, 800.0, 600.0)
    }

    private fun handlePostTurn() {
        isProcessing = true
        Thread {
            Thread.sleep(1000)
            Platform.runLater {
                controller.hideUnmatched()
                updateView()
                isProcessing = false

                // Se modo cooperativo, força a máquina a jogar em seguida
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

            button.style = when (card.isMatchedBy) {
                PlayerType.HUMAN -> "-fx-border-color: blue; -fx-border-width: 3;"
                PlayerType.MACHINE -> "-fx-border-color: red; -fx-border-width: 3;"
                else -> "-fx-border-color: transparent;"
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
        turnLabel.text = "Vez do Jogador: ${if (controller.currentPlayer == PlayerType.HUMAN) "Humano" else "Máquina"}"
        scoreLabel.text = "Pontuação - Humano: ${controller.humanScore}  |  Máquina: ${controller.machineScore}"
    }

    private fun checkGameEnd() {
        if (controller.isGameOver()) {
            timerThread?.interrupt() // Para o cronômetro se vencer antes do tempo acabar

            val winner = when (controller.mode) {
                GameMode.COMPETITIVE -> when {
                    controller.humanScore > controller.machineScore -> "Você venceu! 🎉"
                    controller.machineScore > controller.humanScore -> "A máquina venceu! 🤖"
                    else -> "Empate! 😐"
                }
                GameMode.COOPERATIVE -> "Parabéns! Vocês venceram juntos! 🎉"
            }

            val alert = Alert(Alert.AlertType.INFORMATION).apply {
                title = "Fim de Jogo"
                headerText = winner
                contentText = "Placar final:\nHumano: ${controller.humanScore}  |  Máquina: ${controller.machineScore}"
            }

            alert.showAndWait()

            val menu = MenuView(stage)
            stage.scene = javafx.scene.Scene(menu, 800.0, 600.0)
        }
    }
}
