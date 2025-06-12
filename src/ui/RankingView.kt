package ui

import db.DatabaseManager
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import model.GameMode

class RankingView : BorderPane() {

    init {
        padding = Insets(30.0)

        val coopRanking = DatabaseManager.getTop5Aggregated(GameMode.COOPERATIVE)
        val compRanking = DatabaseManager.getTop5Aggregated(GameMode.COMPETITIVE)

        val coopBox = createRankingBox("Modo Cooperativo", coopRanking)
        val compBox = createRankingBox("Modo Competitivo", compRanking)

        val rankingsHBox = HBox(60.0, coopBox, compBox).apply {
            alignment = Pos.TOP_CENTER
        }

        top = Label("🏆 Ranking de Jogadores").apply {
            font = Font.font("Arial", FontWeight.EXTRA_BOLD, 28.0)
            BorderPane.setAlignment(this, Pos.CENTER)
        }

        center = rankingsHBox
    }

    private fun createRankingBox(title: String, data: List<db.AggregatedRankingEntry>): VBox {
        val box = VBox(15.0).apply {
            alignment = Pos.TOP_LEFT
            padding = Insets(10.0)
        }

        val titleLabel = Label(title).apply {
            font = Font.font("Arial", FontWeight.BOLD, 20.0)
        }

        box.children.add(titleLabel)

        if (data.isEmpty()) {
            box.children.add(Label("Nenhum dado disponível."))
        } else {
            data.forEachIndexed { index, entry ->
                val label = Label(
                    """
                    ${index + 1}. ${entry.playerName}
                    Pontos: ${entry.totalScore}
                    Partidas: ${entry.totalGames}
                    Vitórias: ${entry.victories} | Derrotas: ${entry.defeats} | Empates: ${entry.draws}
                    Média: %.2f
                    Última: ${entry.lastDate} (${entry.lastDifficulty})
                    """.trimIndent().format(entry.averageScore)
                ).apply {
                    font = Font.font("Monospaced", 13.5)
                    isWrapText = true
                }

                val separator = Separator()
                box.children.addAll(label, separator)
            }
        }

        return box
    }
}
