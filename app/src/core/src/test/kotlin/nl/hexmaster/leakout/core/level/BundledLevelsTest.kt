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
