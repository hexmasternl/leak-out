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
    fun `every bundled level fits in 25 columns by 15 rows`() {
        val oversized = levelFiles().map(::parse).filter { it.grid.columns > 25 || it.grid.rows > 15 }.map { "${it.id}: ${it.grid}" }
        assertEquals(emptyList(), oversized)
    }

    @Test
    fun `bundled set has no duplicate ids`() {
        assertEquals(emptyList(), LevelValidator.validateSet(levelFiles().map(::parse)).errors)
    }

    @Test
    fun `every bundled level uses one of the four reservoir types`() {
        val other = levelFiles().map(::parse).filter { ReservoirType.fromFactor(it.flow.reservoirSpeedFactor) == null }
        assertEquals(emptyList(), other.map { "${it.id}: ${it.flow.reservoirSpeedFactor}" })
    }

    @Test
    fun `the faster the liquid, the lower the reservoir factor`() {
        val levels = levelFiles().map(::parse).sortedBy { it.flow.cellsPerSecond }
        val violations = levels.zipWithNext().filter { (slower, faster) ->
            faster.flow.cellsPerSecond > slower.flow.cellsPerSecond && faster.flow.reservoirSpeedFactor > slower.flow.reservoirSpeedFactor
        }.map { (slower, faster) -> "level ${faster.id} is faster than ${slower.id} but slows less" }
        assertEquals(emptyList(), violations)
    }

    @Test
    fun `bundled levels follow the speed bands for their reservoir type`() {
        val wrong = levelFiles().map(::parse).filter {
            val speed = it.flow.cellsPerSecond
            val expected = when {
                speed < 0.2 -> ReservoirType.ORANGE_40
                speed < 0.3 -> ReservoirType.PURPLE_30
                speed < 0.37 -> ReservoirType.BLUE_20
                else -> ReservoirType.RED_10
            }
            ReservoirType.fromFactor(it.flow.reservoirSpeedFactor) != expected
        }
        assertEquals(emptyList(), wrong.map { "${it.id}: speed ${it.flow.cellsPerSecond}, factor ${it.flow.reservoirSpeedFactor}" })
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

    @Test
    fun `leaky tiles come in at level 25 and only on some levels`() {
        val levels = levelFiles().map(::parse).associateBy { it.id }
        assertEquals(emptyList(), (1..24).filter { levels.getValue(it).leaky.isNotEmpty() }, "levels 1 to 24 with leaky tiles")
        val first = levels.getValue(25)
        assertTrue(first.leaky.isNotEmpty(), "level 25 has no leaky tile")
        assertEquals(true, assertIs<SolverResult.Solvable>(LevelSolver.solve(first)).leakFree, "level 25 has no leak-free route")
        val withLeaky = (25..96).filter { levels.getValue(it).leaky.isNotEmpty() }
        assertTrue(withLeaky.size * 2 < (25..96).count(), "levels 25 to 96 with leaky tiles: $withLeaky")
    }

    @Test
    fun `from level 49 some levels offer a shortcut over a leaky tile next to a longer leak-free route`() {
        val levels = levelFiles().map(::parse).associateBy { it.id }
        val shortcuts = (49..96).map(levels::getValue).filter { it.leaky.isNotEmpty() && it.mandatory.isEmpty() }.filter { level ->
            val shortest = (LevelSolver.solve(level) as? SolverResult.Solvable)?.path ?: return@filter false
            val leakFree = (LevelSolver.solve(level.copy(blocked = level.blocked + level.leaky, leaky = emptyList())) as? SolverResult.Solvable)?.path
            leakFree != null && shortest.size < leakFree.size && shortest.any(level::isLeaky)
        }
        assertTrue(shortcuts.size >= 4, "levels from 49 with a leaky shortcut: ${shortcuts.map { it.id }}")
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
