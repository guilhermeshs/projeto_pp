// Main.kt
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import ui.GameView

class Main : Application() {
    override fun start(primaryStage: Stage) {
        val root = GameView()
        primaryStage.title = "Jogo da Memória - Versão 1"
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
