package nl.hexmaster.leakout.scores.online

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A score server sign-in: the bearer token and when it stops being accepted. */
data class Session(val accessToken: String, val expiresAt: Instant)

/** Why a score server call failed; decides whether a queued score is kept or dropped. */
sealed class ScoreServerException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 401: the token (or the server auth code) was not accepted. */
    class Unauthorized(message: String) : ScoreServerException(message)

    /** Another 4xx: the server will never accept this request, so sending it again is pointless. */
    class Rejected(message: String) : ScoreServerException(message)

    /** Offline, a timeout, a 5xx or anything else that may work later. */
    class Unavailable(message: String, cause: Throwable? = null) : ScoreServerException(message, cause)
}

/** The two score server endpoints the game uses (see the `leak-out-server` README). */
interface ScoreServerApi {
    /** `POST /api/players/sessions` with a one-time Play Games server auth code. */
    suspend fun createSession(serverAuthCode: String): Session

    /** `POST /api/scores` for the signed-in player; the server keeps only the best per level. */
    suspend fun submitScore(accessToken: String, level: Int, score: Int)
}

/** The JSON bodies of [ScoreServerApi], kept apart from the HTTP code so they can be tested alone. */
internal object ScoreServerJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun sessionRequest(serverAuthCode: String): String = buildJsonObject { put("serverAuthCode", serverAuthCode) }.toString()

    fun scoreRequest(level: Int, score: Int): String = buildJsonObject {
        put("level", level)
        put("score", score)
    }.toString()

    /** Reads `{ accessToken, expiresAt, ... }`; anything unreadable is the server's fault, not the player's. */
    fun session(body: String): Session = try {
        val fields = json.parseToJsonElement(body).jsonObject
        Session(
            accessToken = fields.getValue("accessToken").jsonPrimitive.content,
            expiresAt = OffsetDateTime.parse(fields.getValue("expiresAt").jsonPrimitive.content).toInstant(),
        )
    } catch (error: Exception) {
        throw ScoreServerException.Unavailable("unreadable session response", error)
    }
}
