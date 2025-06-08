package controller

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun play(path: String, loop: Boolean = true) {
        stop()
        val resource = javaClass.getResource(path)
            ?: throw IllegalArgumentException("Recurso de áudio não encontrado: $path")
        val media = Media(resource.toString())
        mediaPlayer = MediaPlayer(media).apply {
            if (loop) {
                cycleCount = MediaPlayer.INDEFINITE
            }
            play()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer = null
    }
}
