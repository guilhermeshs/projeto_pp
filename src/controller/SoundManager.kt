package controller

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null
    var isMuted: Boolean = false
        private set

    fun play(path: String, loop: Boolean = true) {
        stop()
        val resource = javaClass.getResource(path)
            ?: throw IllegalArgumentException("Recurso de áudio não encontrado: $path")
        val media = Media(resource.toString())
        mediaPlayer = MediaPlayer(media).apply {
            isMute = isMuted
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

    fun toggleMute() {
        isMuted = !isMuted
        mediaPlayer?.isMute = isMuted
    }
}
