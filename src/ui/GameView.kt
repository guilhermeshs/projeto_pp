package ui

import controller.GameController
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import model.Card
import model.PlayerType

class GameView : BorderPane() {

    private val rows = 6
    private val cols = 4
    private val controller = GameController(rows * cols)
    private var isProcessing = false
    private val buttons = mutableMapOf<Card, Button>()

    private val turnLabel = Label()
    private val scoreLabel = Label()

    init {
        val topPanel = VBox(10.0, turnLabel, scoreLabel)
        topPanel.padding = Insets(10.0)
        top = topPanel

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0)
        }

        controller.cards.forEachIndexed { index, card ->
            val button = Button("❓").apply {
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

        center = grid
        updateLabels()
    }

    private fun handlePostTurn() {
        isProcessing = true

        Thread {
            Thread.sleep(1000)

            Platform.runLater {
                controller.hideUnmatched()
                updateView()
                isProcessing = false

                if (controller.isMachineTurn()) {
                    handleMachineTurn()
                }
            }
        }.start()
    }

    private fun handleMachineTurn() {
        isProcessing = true

        Thread {
            Thread.sleep(1000)

            val selected = controller.playMachineTurn()

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
            // Define o texto da carta
            button.text = if (card.isMatched || card.isRevealed) card.symbol else "❓"

            // Desabilita carta se já combinada
            button.isDisable = card.isMatched

            // Estilo da borda:
            button.style = when {
                card.isMatchedBy == PlayerType.HUMAN -> "-fx-border-color: blue; -fx-border-width: 3px;"
                card.isMatchedBy == PlayerType.MACHINE -> "-fx-border-color: red; -fx-border-width: 3px;"
                card.isRevealed -> when (controller.currentPlayer) {
                    PlayerType.HUMAN -> "-fx-border-color: blue; -fx-border-width: 3px;"
                    PlayerType.MACHINE -> "-fx-border-color: red; -fx-border-width: 3px;"
                }
                else -> ""
            }
        }

        updateLabels()
        checkGameEnd()
    }

    private fun checkGameEnd() {
        if (controller.isGameOver()) {
            val winner = when {
                controller.humanScore > controller.machineScore -> "Você venceu! 🎉"
                controller.machineScore > controller.humanScore -> "A máquina venceu! 🤖"
                else -> "Empate! 😐"
            }

            val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
            alert.title = "Fim de Jogo"
            alert.headerText = winner
            alert.contentText = "Placar final:\nHumano: ${controller.humanScore}\nMáquina: ${controller.machineScore}"
            alert.showAndWait()
        }
    }



    private fun updateLabels() {
        turnLabel.text = "Vez do Jogador: ${if (controller.currentPlayer == PlayerType.HUMAN) "Humano" else "Máquina"}"
        scoreLabel.text = "Pontuação - Humano: ${controller.humanScore}  |  Máquina: ${controller.machineScore}"
    }
}
