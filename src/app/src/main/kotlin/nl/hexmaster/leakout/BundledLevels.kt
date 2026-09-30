package nl.hexmaster.leakout

import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.util.Locale
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.ui.levels.LevelCatalog

/**
 * The levels shipped in `assets/levels/level_NNN.json`, by id. The level library counts them
 * through [LevelCatalog]; the game loads the one picked and the one after a win.
 */
class BundledLevels(private val assets: AssetManager) : LevelCatalog {
    private val count: Int by lazy { contiguousCount(assets.list(DIRECTORY).orEmpty().asList()) }

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

    override fun levelCount(): Int = count

    companion object {
        private const val DIRECTORY = "levels"

        fun path(id: Int): String = String.format(Locale.ROOT, "$DIRECTORY/level_%03d.json", id)

        /** How many levels `1..n` are present among [fileNames] without a gap; a gap ends the count. */
        fun contiguousCount(fileNames: Collection<String>): Int {
            val names = fileNames.toSet()
            var id = 0
            while (path(id + 1).removePrefix("$DIRECTORY/") in names) id++
            return id
        }
    }
}
