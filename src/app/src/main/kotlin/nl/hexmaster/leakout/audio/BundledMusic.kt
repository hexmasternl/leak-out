package nl.hexmaster.leakout.audio

import android.content.res.AssetManager
import android.util.Log

/** The background music shipped in `assets/music/`: the tracks and `audio_credits.json`, which is both playlist and credits. */
object BundledMusic {
    const val ASSET_DIR = "music"
    const val CREDITS_FILE = "audio_credits.json"

    /** The bundled playlist, or null when the credits file is missing or invalid. Music is optional, so this never throws. */
    fun load(assets: AssetManager): Playlist? =
        runCatching { assets.open("$ASSET_DIR/$CREDITS_FILE").bufferedReader().use { it.readText() } }
            .onFailure { Log.w("BundledMusic", "Cannot read $CREDITS_FILE", it) }
            .getOrNull()
            ?.let(AudioCreditsParser::parse)
}
