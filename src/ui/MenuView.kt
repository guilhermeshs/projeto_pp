package ui

import controller.GameController
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import model.Difficulty
import model.GameMode
import controller.SoundManager

class MenuView(private val stage: Stage) : VBox() {

    private val modeSelector = ComboBox<GameMode>()
    private val difficultySelector = ComboBox<Difficulty>()
    private val startButton = Button("Iniciar Jogo")
    private val exitButton = Button("Sair do Jogo") // NOVO BOTÃO

    init {
        SoundManager.play("/sounds/menu.mp3")
        spacing = 20.0
        alignment = Pos.CENTER

        val muteButton = createMuteButton()

        children.addAll(
            Label("Modo de Jogo:"),
            modeSelector,
            Label("Nível de Dificuldade:"),
            difficultySelector,
            Label("Opções:"),
            startButton,
            exitButton,       // ADICIONADO AQUI
            muteButton
        )

        modeSelector.items.addAll(GameMode.values())
        modeSelector.selectionModel.selectFirst()

        difficultySelector.items.addAll(Difficulty.values())
        difficultySelector.selectionModel.selectFirst()

        startButton.setOnAction {
            val selectedMode = modeSelector.value
            val selectedDifficulty = difficultySelector.value

            val controller = GameController(selectedMode, selectedDifficulty)
            val gameView = GameView(controller, stage)
            val scene = Scene(gameView, 1280.0, 720.0)
            scene.stylesheets.add(javaClass.getResource("/style.css")!!.toExternalForm())
            stage.scene = scene
        }

        // AÇÃO DO BOTÃO DE SAIR
        exitButton.setOnAction {
            Platform.exit()
        }
    }
}
