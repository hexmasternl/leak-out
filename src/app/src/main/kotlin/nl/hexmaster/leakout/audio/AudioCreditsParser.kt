package nl.hexmaster.leakout.audio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads `audio_credits.json`: an array of `{ "title", "artist", "file" }` in playback order.
 * The same file is the playlist and the credits on the About panel.
 */
object AudioCreditsParser {
    /** The playlist in [json], or null when it is invalid, empty or lists a file twice. Music is optional, so this never throws. */
    fun parse(json: String): Playlist? {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonArray ?: return null
        val tracks = root.map { entry ->
            val fields = entry as? JsonObject ?: return null
            Track(
                file = fields.text("file") ?: return null,
                title = fields.text("title") ?: return null,
                artist = fields.text("artist") ?: return null,
            )
        }
        return runCatching { Playlist(tracks) }.getOrNull()
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
