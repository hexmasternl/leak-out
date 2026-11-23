package nl.hexmaster.leakout.scores.online

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which rows the daily leaderboard shows: the top 5 and the player's own row (spec `daily-pipe`). */
class DailyLeaderboardRowsTest {
    private val date = LocalDate.of(2026, 11, 23)
    private val names = listOf("Player v", "Player w", "Player x", "Player y", "Player z", "Six", "Seven", "Eight", "Nine", "Ten")
    private val scores = listOf(512, 511, 501, 498, 495, 480, 470, 460, 450, 440)

    private fun entries(count: Int, current: Int? = null) = (1..count).map { position ->
        DailyLeaderboardEntry(position, names[position - 1], scores[position - 1], 2, isCurrentPlayer = position == current)
    }

    private fun row(position: Int, own: Boolean = false) =
        DailyLeaderboardRow(position, names[position - 1], scores[position - 1], isCurrentPlayer = own)

    @Test
    fun `a player outside the top 10 gets a sixth row with their position and best score`() {
        val top = DailyTopScores(date, entries(10), DailyPlayerRank(position = 76, score = 125, stars = 1))

        assertEquals(
            (1..5).map { row(it) } + DailyLeaderboardRow(76, displayName = null, score = 125, isCurrentPlayer = true),
            dailyLeaderboardRows(top),
        )
    }

    @Test
    fun `a player in places 6 to 10 gets a sixth row with their name from the entries`() {
        val top = DailyTopScores(date, entries(10, current = 8), DailyPlayerRank(position = 8, score = 460, stars = 2))

        assertEquals((1..5).map { row(it) } + row(8, own = true), dailyLeaderboardRows(top))
    }

    @Test
    fun `a player in the top 5 is marked in place and gets no extra row`() {
        val top = DailyTopScores(date, entries(10, current = 3), DailyPlayerRank(position = 3, score = 501, stars = 3))

        assertEquals(listOf(row(1), row(2), row(3, own = true), row(4), row(5)), dailyLeaderboardRows(top))
    }

    @Test
    fun `a player without a result today gets no own row`() {
        val top = DailyTopScores(date, entries(10), player = null)

        assertEquals((1..5).map { row(it) }, dailyLeaderboardRows(top))
    }

    @Test
    fun `fewer than 5 results show only the rows there are`() {
        val top = DailyTopScores(date, entries(3), player = null)

        assertEquals((1..3).map { row(it) }, dailyLeaderboardRows(top))
    }

    @Test
    fun `no results give no rows`() {
        assertEquals(emptyList<DailyLeaderboardRow>(), dailyLeaderboardRows(DailyTopScores(date, emptyList(), player = null)))
    }

    @Test
    fun `entries out of order are listed by position`() {
        val top = DailyTopScores(date, entries(5).reversed(), player = null)

        assertEquals((1..5).map { row(it) }, dailyLeaderboardRows(top))
    }
}
