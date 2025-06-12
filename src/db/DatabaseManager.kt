package db

import model.GameMode
import model.Difficulty
import java.sql.DriverManager
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

object DatabaseManager {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:ranking.db")

    init {
        val stmt = connection.createStatement()
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS ranking (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                playerName TEXT,
                score INTEGER,
                mode TEXT,
                difficulty TEXT,
                date TEXT,
                result TEXT
            )
        """.trimIndent()
        )
        stmt.close()
    }

    fun insertRanking(entry: RankingEntry) {
        val sql = """
            INSERT INTO ranking (playerName, score, mode, difficulty, date, result)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, entry.playerName)
            stmt.setInt(2, entry.score)
            stmt.setString(3, entry.mode.name)
            stmt.setString(4, entry.difficulty.name)
            stmt.setString(5, entry.date)
            stmt.setString(6, entry.result)
            stmt.executeUpdate()
        }
    }

    fun getTop5Aggregated(mode: GameMode): List<AggregatedRankingEntry> {
        val result = mutableListOf<AggregatedRankingEntry>()
        val sql = """
        SELECT 
            playerName,
            SUM(score) AS totalScore,
            COUNT(*) AS totalGames,
            SUM(CASE WHEN result = 'Vitória' THEN 1 ELSE 0 END) AS victories,
            SUM(CASE WHEN result = 'Derrota' THEN 1 ELSE 0 END) AS defeats,
            AVG(score) AS averageScore,
            (SELECT difficulty FROM ranking r2 
             WHERE r2.playerName = r1.playerName AND r2.mode = ? 
             ORDER BY r2.date DESC LIMIT 1) AS lastDifficulty,
            (SELECT date FROM ranking r2 
             WHERE r2.playerName = r1.playerName AND r2.mode = ? 
             ORDER BY r2.date DESC LIMIT 1) AS lastDate
        FROM ranking r1
        WHERE mode = ?
        GROUP BY playerName
        ORDER BY totalScore DESC
        LIMIT 5
    """.trimIndent()

        val stmt: PreparedStatement = connection.prepareStatement(sql)
        stmt.setString(1, mode.name)
        stmt.setString(2, mode.name)
        stmt.setString(3, mode.name)

        val rs: ResultSet = stmt.executeQuery()
        while (rs.next()) {
            result.add(
                AggregatedRankingEntry(
                    playerName = rs.getString("playerName"),
                    totalScore = rs.getInt("totalScore"),
                    victories = rs.getInt("victories"),
                    defeats = rs.getInt("defeats"),
                    averageScore = rs.getDouble("averageScore"),
                    lastDifficulty = Difficulty.valueOf(rs.getString("lastDifficulty")),
                    lastDate = rs.getString("lastDate")
                )
            )
        }

        rs.close()
        stmt.close()
        return result
    }

}
