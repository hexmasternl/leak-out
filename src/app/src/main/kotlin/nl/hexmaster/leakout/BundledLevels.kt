package nl.hexmaster.leakout

import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.util.Locale
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.ParseResult

/**
 * The levels shipped in `assets/levels/level_NNN.json`, by id. Until the level library brings its
 * own catalogue, this is how the game finds level 1 and the level after the one just won.
 */
class BundledLevels(private val assets: AssetManager) {
    /** The bundled level with [id], or null when there is none. A bundled level that does not parse is a bug. */
    fun load(id: Int): Level? {
        if (id < 1) return null
        val text = try {
            assets.open(path(id)).bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            return null
        }
        return when (val result = LevelParser.parse(text)) {
            is ParseResult.Success -> result.level
            is ParseResult.Failure -> error("Cannot load level $id: ${result.error.message}")
        }
    }

    /** The level after [level], or null when [level] is the last one. */
    fun next(level: Level): Level? = load(level.id + 1)

    companion object {
        fun path(id: Int): String = String.format(Locale.ROOT, "levels/level_%03d.json", id)
    }
}
