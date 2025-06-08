package ui

import javafx.scene.control.Button
import controller.SoundManager

fun createMuteButton(): Button {
    val button = Button(if (SoundManager.isMuted) "🔇" else "🔊").apply {
        styleClass.add("mute-button")
    }

    button.setOnAction {
        SoundManager.toggleMute()
        button.text = if (SoundManager.isMuted) "🔇" else "🔊"
    }

    return button
}

