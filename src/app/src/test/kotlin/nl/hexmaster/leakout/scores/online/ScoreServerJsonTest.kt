package nl.hexmaster.leakout.scores.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Reading the leaderboard answer of `GET /api/levels/{level}/scores/top`. */
class ScoreServerJsonTest {
    @Test
    fun `a full leaderboard is read in ranking order`() {
        val entries = (1..10).joinToString(",") { """{"position":$it,"displayName":"P$it","score":${1100 - it * 100},"isCurrentPlayer":${it == 4}}""" }
        val board = ScoreServerJson.leaderboard("""{"level":7,"entries":[$entries]}""")
        assertEquals(7, board.level)
        assertEquals((1..10).toList(), board.entries.map { it.position })
        assertEquals(LeaderboardEntry(4, "P4", 700, isCurrentPlayer = true), board.entries[3])
        assertEquals(listOf(4), board.entries.filter { it.isCurrentPlayer }.map { it.position })
    }

    @Test
    fun `an empty leaderboard has no entries`() {
        assertEquals(LevelLeaderboard(3, emptyList()), ScoreServerJson.leaderboard("""{"level":3,"entries":[]}"""))
    }

    @Test
    fun `unknown fields are ignored and entries are sorted by position`() {
        val board = ScoreServerJson.leaderboard(
            """{"level":2,"generatedAt":"now","entries":[
                {"position":2,"displayName":"Bo","score":90,"isCurrentPlayer":false,"avatar":"x"},
                {"position":1,"displayName":"Ann","score":120}]}""",
        )
        assertEquals(listOf(LeaderboardEntry(1, "Ann", 120, false), LeaderboardEntry(2, "Bo", 90, false)), board.entries)
    }

    @Test
    fun `a broken body is unavailable`() {
        for (body in listOf("<html>", "{}", """{"level":1,"entries":[{"position":"one"}]}""")) {
            try {
                ScoreServerJson.leaderboard(body)
                fail("expected a failure for $body")
            } catch (error: ScoreServerException) {
                assertTrue("got $error for $body", error is ScoreServerException.Unavailable)
            }
        }
    }
}
