import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import ui.MenuView

class Main : Application() {
    override fun start(primaryStage: Stage) {
        val menu = MenuView(primaryStage)

        val scene = Scene(menu, 1280.0, 720.0)
        scene.stylesheets.add(javaClass.getResource("/style.css")!!.toExternalForm())
        primaryStage.title = "Jogo da Memória - Menu"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
