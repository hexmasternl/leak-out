package nl.hexmaster.leakout.scores.online

import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import nl.hexmaster.leakout.achievements.playgames.PlayGamesLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState

/** The player's daily pipe streak: consecutive UTC days with a win up to today, the best ever, and whether today is won. */
data class DailyStreak(val current: Int, val best: Int, val wonToday: Boolean)

/** A player's best result on a daily pipe. */
data class DailyBestScore(val score: Int, val stars: Int)

/**
 * Today's daily pipe as the server sends it: the server's UTC [date], the level in the level-format
 * JSON ([levelJson], still to be parsed and validated), the seed of the stack, and the player's
 * streak and best result.
 */
data class DailyPipe(
    val date: LocalDate,
    val levelJson: String,
    val stackSeed: Long,
    val streak: DailyStreak,
    val bestScore: DailyBestScore?,
)

/** The answer to a daily pipe submission: the stored best, whether this became it, the player's rank and streak. */
data class DailyScoreResult(
    val date: LocalDate,
    val bestScore: DailyBestScore,
    val isNewBest: Boolean,
    val rank: Int,
    val streak: DailyStreak,
)

/** One ranked best result of a daily pipe. */
data class DailyLeaderboardEntry(val position: Int, val displayName: String, val score: Int, val stars: Int, val isCurrentPlayer: Boolean)

/** The asking player's own place on a daily pipe: their [position] and best [score] and [stars]. */
data class DailyPlayerRank(val position: Int, val score: Int, val stars: Int)

/** The top of a daily pipe's ranking and the asking player's own place, or null when they have no result that day. */
data class DailyTopScores(val date: LocalDate, val entries: List<DailyLeaderboardEntry>, val player: DailyPlayerRank?)

/** The daily pipe endpoints of the score server (server README, "Daily pipe"). */
interface DailyPipeServerApi {
    /** `GET /api/daily-pipes/today`. A 404 (no pipe yet) is a [ScoreServerException.Rejected] with status 404. */
    suspend fun todaysDailyPipe(accessToken: String): DailyPipe

    /** `POST /api/daily-pipes/{date}/scores`. A 409 (the day has ended) is a [ScoreServerException.Rejected] with status 409. */
    suspend fun submitDailyScore(accessToken: String, date: LocalDate, score: Int, stars: Int): DailyScoreResult

    /** `GET /api/daily-pipes/{date}/scores/top`. */
    suspend fun dailyTopScores(accessToken: String, date: LocalDate): DailyTopScores
}

/** The JSON bodies of [DailyPipeServerApi]; anything unreadable is the server's fault, not the player's. */
internal object DailyPipeJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun scoreRequest(score: Int, stars: Int): String = buildJsonObject {
        put("score", score)
        put("stars", stars)
    }.toString()

    /** Reads `{ date, level: {…}, stackSeed, streak, bestScore }`; the level stays JSON for `LevelParser`. */
    fun dailyPipe(body: String): DailyPipe = readOrUnavailable("daily pipe") {
        val fields = json.parseToJsonElement(body).jsonObject
        DailyPipe(
            date = LocalDate.parse(fields.getValue("date").jsonPrimitive.content),
            levelJson = fields.getValue("level").jsonObject.toString(),
            stackSeed = fields.getValue("stackSeed").jsonPrimitive.long,
            streak = streak(fields.getValue("streak")),
            bestScore = fields["bestScore"]?.takeUnless { it is JsonNull }?.let(::bestScore),
        )
    }

    /** Reads `{ date, bestScore: { score, stars }, isNewBest, rank, streak }`. */
    fun scoreResult(body: String): DailyScoreResult = readOrUnavailable("daily score") {
        val fields = json.parseToJsonElement(body).jsonObject
        DailyScoreResult(
            date = LocalDate.parse(fields.getValue("date").jsonPrimitive.content),
            bestScore = bestScore(fields.getValue("bestScore")),
            isNewBest = fields.getValue("isNewBest").jsonPrimitive.boolean,
            rank = fields.getValue("rank").jsonPrimitive.int,
            streak = streak(fields.getValue("streak")),
        )
    }

    /** Reads `{ date, entries: [{ position, displayName, score, stars, isCurrentPlayer }], player: { position, score, stars } }`, sorted by position. */
    fun topScores(body: String): DailyTopScores = readOrUnavailable("daily top scores") {
        val fields = json.parseToJsonElement(body).jsonObject
        DailyTopScores(
            date = LocalDate.parse(fields.getValue("date").jsonPrimitive.content),
            entries = fields.getValue("entries").jsonArray.map { element ->
                val entry = element.jsonObject
                DailyLeaderboardEntry(
                    position = entry.getValue("position").jsonPrimitive.int,
                    displayName = entry.getValue("displayName").jsonPrimitive.content,
                    score = entry.getValue("score").jsonPrimitive.int,
                    stars = entry.getValue("stars").jsonPrimitive.int,
                    isCurrentPlayer = entry["isCurrentPlayer"]?.jsonPrimitive?.boolean ?: false,
                )
            }.sortedBy { it.position },
            player = (fields["player"] as? JsonObject)?.let { player ->
                DailyPlayerRank(
                    position = player.getValue("position").jsonPrimitive.int,
                    score = player.getValue("score").jsonPrimitive.int,
                    stars = player.getValue("stars").jsonPrimitive.int,
                )
            },
        )
    }

    private fun streak(element: JsonElement): DailyStreak {
        val fields = element.jsonObject
        return DailyStreak(
            current = fields.getValue("current").jsonPrimitive.int,
            best = fields.getValue("best").jsonPrimitive.int,
            wonToday = fields.getValue("wonToday").jsonPrimitive.boolean,
        )
    }

    private fun bestScore(element: JsonElement): DailyBestScore {
        val fields = element.jsonObject
        return DailyBestScore(fields.getValue("score").jsonPrimitive.int, fields.getValue("stars").jsonPrimitive.int)
    }

    private inline fun <T> readOrUnavailable(what: String, read: () -> T): T = try {
        read()
    } catch (error: Exception) {
        throw ScoreServerException.Unavailable("unreadable $what response", error)
    }
}

/** What downloading today's daily pipe gave. */
sealed interface DailyPipeDownload {
    data class Available(val pipe: DailyPipe) : DailyPipeDownload

    /** The server has not published today's pipe yet. */
    data object NotPublished : DailyPipeDownload

    /** Not signed in, offline, a timeout or a server error. */
    data object Failed : DailyPipeDownload
}

/** What submitting a daily pipe result gave. */
sealed interface DailySubmission {
    /** The server took the result: the player's best, rank and streak. */
    data class Accepted(val result: DailyScoreResult) : DailySubmission

    /** The pipe's day has ended (UTC midnight passed); nothing was stored. */
    data object Ended : DailySubmission

    /** Offline, a timeout or a server error; trying again may work. */
    data object Failed : DailySubmission
}

/** What loading a daily pipe's top scores gave. */
sealed interface DailyTopResult {
    data class Loaded(val top: DailyTopScores) : DailyTopResult

    /** Not signed in, offline, a timeout or a server error. */
    data object Failed : DailyTopResult
}

/** The daily pipe for the game: today's pipe, submitting a result and the day's top scores. */
interface DailyPipes {
    suspend fun today(): DailyPipeDownload

    suspend fun submit(date: LocalDate, score: Int, stars: Int): DailySubmission

    /** The top scores of the pipe of [date] and the player's own place, for the daily leaderboard. */
    suspend fun top(date: LocalDate): DailyTopResult
}

/**
 * The daily pipe client on top of the score server sign-in of [session] (the same one as score
 * posting and the top 10): a 401 signs in to the server again once. Only a player signed in with
 * Play Games can use it; the daily pipe is never cached, it is downloaded every time.
 */
class DailyPipeApi(
    private val api: DailyPipeServerApi,
    private val session: ScoreServerSession,
    private val signIn: StateFlow<SignInState>,
    private val serverAuthCodes: StateFlow<ServerAuthCodeSource?>,
    private val log: PlayGamesLog = PlayGamesLog.Android,
) : DailyPipes {
    override suspend fun today(): DailyPipeDownload = try {
        DailyPipeDownload.Available(authorized { token -> api.todaysDailyPipe(token) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (notYet: ScoreServerException.Rejected) {
        if (notYet.status == 404) DailyPipeDownload.NotPublished else failed("daily pipe", notYet, DailyPipeDownload.Failed)
    } catch (error: Exception) {
        failed("daily pipe", error, DailyPipeDownload.Failed)
    }

    override suspend fun submit(date: LocalDate, score: Int, stars: Int): DailySubmission = try {
        DailySubmission.Accepted(authorized { token -> api.submitDailyScore(token, date, score, stars) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (ended: ScoreServerException.Rejected) {
        if (ended.status == 409) DailySubmission.Ended else failed("daily score", ended, DailySubmission.Failed)
    } catch (error: Exception) {
        failed("daily score", error, DailySubmission.Failed)
    }

    override suspend fun top(date: LocalDate): DailyTopResult = try {
        DailyTopResult.Loaded(authorized { token -> api.dailyTopScores(token, date) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        failed("daily top scores", error, DailyTopResult.Failed)
    }

    private suspend fun <T> authorized(call: suspend (token: String) -> T): T {
        if (signIn.value !is SignInState.SignedIn) throw ScoreServerException.Unavailable("not signed in to Play Games")
        val codes = serverAuthCodes.value ?: throw ScoreServerException.Unavailable("no activity for a server auth code")
        return try {
            call(session.token(codes))
        } catch (expired: ScoreServerException.Unauthorized) {
            session.invalidate()
            call(session.token(codes))
        }
    }

    private fun <T> failed(what: String, error: Exception, result: T): T {
        log.failed(what, error)
        return result
    }
}
