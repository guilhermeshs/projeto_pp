package controller

import javafx.application.Platform
import javafx.scene.control.Label

class GameTimer(
    private var totalSeconds: Int,
    private val label: Label,
    private val onTimeOver: () -> Unit
) {
    private var thread: Thread? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            while (totalSeconds > 0 && running) {
                Thread.sleep(1000)
                totalSeconds--
                Platform.runLater { updateLabel() }
            }

            if (running && totalSeconds == 0) {
                Platform.runLater { onTimeOver() }
            }
        }
        thread?.start()
    }

    private fun updateLabel() {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        label.text = "Tempo restante: %02d:%02d".format(minutes, seconds)
    }

    fun stop() {
        running = false
        thread?.interrupt()
    }
}
