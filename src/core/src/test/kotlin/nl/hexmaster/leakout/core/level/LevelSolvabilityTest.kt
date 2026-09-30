package nl.hexmaster.leakout.core.level

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Every level in the levels directory must be solvable. The directory comes from the
 * `leakout.levelsDir` system property, so the same suite covers the bundled levels as well as any
 * other directory of `level_NNN.json` files it is pointed at.
 */
class LevelSolvabilityTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("levelFileNames")
    fun `level is solvable`(fileName: String) {
        val file = File(levelsDir, fileName)
        val level = assertIs<ParseResult.Success>(LevelParser.parse(file.readText()), "$fileName does not parse").level
        when (val result = LevelSolver.solve(level)) {
            is SolverResult.Solvable -> Unit
            is SolverResult.Unsolvable ->
                fail("$fileName (id ${level.id}, '${level.name}') is unsolvable: ${result.reason}")
        }
    }

    companion object {
        private val levelsDir: File by lazy {
            File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        }

        @JvmStatic
        fun levelFileNames(): List<String> =
            levelsDir.listFiles { file -> file.name.matches(Regex("""level_\d{3}\.json""")) }
                .orEmpty().map { it.name }.sorted()
    }
}
