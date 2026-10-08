package nl.hexmaster.leakout.core.level

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/** Every level shipped in the app's assets must parse and validate. */
class BundledLevelsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("levelFileNames")
    fun `bundled level parses and validates`(fileName: String) {
        val level = parse(File(levelsDir, fileName))
        assertEquals(emptyList(), LevelValidator.validate(level).errors)
    }

    @Test
    fun `at least the first three levels are bundled`() {
        assertTrue(levelFiles().size >= 3, "found ${levelFiles().map { it.name }} in $levelsDir")
    }

    @Test
    fun `bundled set has no duplicate ids`() {
        assertEquals(emptyList(), LevelValidator.validateSet(levelFiles().map(::parse)).errors)
    }

    @Test
    fun `portals come in from level 49 and only on some levels`() {
        val levels = levelFiles().map(::parse).associateBy { it.id }
        assertEquals(emptyList(), (1..48).filter { levels.getValue(it).portals.isNotEmpty() }, "levels 1 to 48 with portals")
        val first = levels.getValue(49)
        assertEquals(1, first.portals.size, "portal pairs on level 49")
        val path = assertIs<SolverResult.Solvable>(LevelSolver.solve(first)).path
        val pair = first.portals.single()
        assertTrue(pair.a.position in path && pair.b.position in path, "the shortest route of level 49 does not pass its pair: $path")
        val later = (50..96).filter { levels.getValue(it).portals.isNotEmpty() }
        assertTrue(later.size * 2 < (50..96).count(), "levels 50 to 96 with portals: $later")
        assertTrue(levels.values.all { it.portals.size <= LevelValidator.MAX_PORTAL_PAIRS })
    }

    companion object {
        private val levelsDir: File by lazy {
            File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        }

        private fun levelFiles(): List<File> =
            levelsDir.listFiles { file -> file.name.matches(Regex("""level_\d{3}\.json""")) }.orEmpty().sortedBy { it.name }

        @JvmStatic
        fun levelFileNames(): List<String> = levelFiles().map { it.name }

        private fun parse(file: File): Level =
            assertIs<ParseResult.Success>(LevelParser.parse(file.readText()), "${file.name} does not parse").level
    }
}
