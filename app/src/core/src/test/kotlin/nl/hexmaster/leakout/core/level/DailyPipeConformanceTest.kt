package nl.hexmaster.leakout.core.level

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The game server generates the daily pipe and checks it with a C# port of [LevelValidator] and
 * [LevelSolver]. Both sides check the shared fixtures in the server's test project against one file
 * of expected verdicts, so the port cannot drift from these originals. Levels the server's generator
 * produced must also parse, validate and solve here, because the app plays them.
 */
class DailyPipeConformanceTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureNames")
    fun `fixture matches its verdict`(fileName: String) {
        val expected = verdicts[fileName] ?: fail("$fileName has no verdict in verdicts.json")
        assertEquals(expected, judge(File(fixturesDir, "levels/$fileName")))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedNames")
    fun `generated daily pipe parses, validates and is solvable`(fileName: String) {
        val level = parse(File(fixturesDir, "generated/$fileName"))
        assertEquals(emptyList(), LevelValidator.validate(level).errors)
        assertIs<SolverResult.Solvable>(LevelSolver.solve(level))
    }

    @Test
    fun `every verdict has a fixture`() {
        val files = fixtureNames().toSet()
        assertEquals(emptyList(), verdicts.keys.filter { it !in files })
    }

    @Test
    fun `the server has written generated daily pipes`() {
        assertTrue(generatedNames().size >= 7, "found ${generatedNames()} in $fixturesDir/generated")
    }

    /** The verdict on one fixture, in the shape of `verdicts.json`. */
    private data class Verdict(
        val valid: Boolean,
        val errorFields: List<String> = emptyList(),
        val solvable: Boolean? = null,
        val leakFree: Boolean? = null,
        val routeLength: Int? = null,
    )

    private fun judge(file: File): Verdict {
        val level = parse(file)
        val errors = LevelValidator.validate(level).errors
        if (errors.isNotEmpty()) return Verdict(valid = false, errorFields = errors.map { it.field })
        return when (val result = LevelSolver.solve(level)) {
            is SolverResult.Solvable -> Verdict(true, solvable = true, leakFree = result.leakFree, routeLength = result.path.size)
            is SolverResult.Unsolvable -> Verdict(true, solvable = false)
        }
    }

    private fun parse(file: File): Level = when (val result = LevelParser.parse(file.readText())) {
        is ParseResult.Success -> result.level
        is ParseResult.Failure -> fail("${file.name}: ${result.error}")
    }

    companion object {
        private val fixturesDir: File = File(
            System.getProperty("leakout.dailyPipeFixturesDir") ?: error("leakout.dailyPipeFixturesDir is not set"),
        )

        private val verdicts: Map<String, Verdict> by lazy {
            Json.parseToJsonElement(File(fixturesDir, "verdicts.json").readText()).jsonObject.mapValues { (_, value) ->
                val verdict = value.jsonObject
                Verdict(
                    valid = verdict.boolean("valid") ?: fail("verdict without 'valid'"),
                    errorFields = verdict["errorFields"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                    solvable = verdict.boolean("solvable"),
                    leakFree = verdict.boolean("leakFree"),
                    routeLength = verdict["routeLength"]?.jsonPrimitive?.intOrNull,
                )
            }
        }

        private fun JsonObject.boolean(name: String): Boolean? = this[name]?.jsonPrimitive?.booleanOrNull

        private fun names(dir: String): List<String> =
            File(fixturesDir, dir).listFiles { file -> file.extension == "json" }.orEmpty().map { it.name }.sorted()

        @JvmStatic
        fun fixtureNames(): List<String> = names("levels")

        @JvmStatic
        fun generatedNames(): List<String> = names("generated")
    }
}
