package nl.hexmaster.leakout.scores.online

/** How many of the best results the daily leaderboard lists before the player's own row. */
const val DAILY_LEADERBOARD_ROWS = 5

/**
 * One row of the daily leaderboard. [displayName] is null only for the player's own row when the
 * server sent no entry for that position; the leaderboard then shows the player's own name.
 */
data class DailyLeaderboardRow(val position: Int, val displayName: String?, val score: Int, val isCurrentPlayer: Boolean)

/**
 * The rows of the daily leaderboard (spec `daily-pipe`): the best [rows] results in order and, when
 * the player has a result for the day but is not among them, a row for the player below them with
 * their position and best score. A player among the best keeps their row there, marked as theirs.
 */
fun dailyLeaderboardRows(top: DailyTopScores, rows: Int = DAILY_LEADERBOARD_ROWS): List<DailyLeaderboardRow> {
    val best = top.entries.sortedBy { it.position }.take(rows)
        .map { DailyLeaderboardRow(it.position, it.displayName, it.score, it.isCurrentPlayer) }
    val player = top.player ?: return best
    if (best.any { it.isCurrentPlayer } || player.position <= rows) return best
    // The server sends the top 10, so a player in places 6 to 10 has their name there.
    val name = top.entries.firstOrNull { it.isCurrentPlayer }?.displayName
    return best + DailyLeaderboardRow(player.position, name, player.score, isCurrentPlayer = true)
}
