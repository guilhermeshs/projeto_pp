package util

import javafx.scene.Parent
import javafx.scene.Scene

class SceneUtils

fun createStyledScene(root: Parent, width: Double, height: Double): Scene {
    val scene = Scene(root, width, height)
    scene.stylesheets.add(SceneUtils::class.java.getResource("/style.css")!!.toExternalForm())
    return scene
}
