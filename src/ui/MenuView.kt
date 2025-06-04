package ui

import controller.GameController
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import model.Difficulty
import model.GameMode

class MenuView(private val stage: Stage) : VBox() {

    private val modeSelector = ComboBox<GameMode>()
    private val difficultySelector = ComboBox<Difficulty>()
    private val startButton = Button("Iniciar Jogo")

    init {
        spacing = 20.0
        alignment = Pos.CENTER

        children.addAll(
            Label("Modo de Jogo:"),
            modeSelector,
            Label("Nível de Dificuldade:"),
            difficultySelector,
            startButton
        )

        // Preencher seletores
        modeSelector.items.addAll(GameMode.values())
        modeSelector.selectionModel.selectFirst()

        difficultySelector.items.addAll(Difficulty.values())
        difficultySelector.selectionModel.selectFirst()

        // Ação do botão
        startButton.setOnAction {
            val selectedMode = modeSelector.value
            val selectedDifficulty = difficultySelector.value

            val controller = GameController(selectedMode, selectedDifficulty)
            val gameView = GameView(controller, stage)
            val scene = Scene(gameView, 800.0, 600.0)
            stage.scene = scene
        }
    }
}
