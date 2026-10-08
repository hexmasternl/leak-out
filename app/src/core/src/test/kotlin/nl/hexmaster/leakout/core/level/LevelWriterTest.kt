package nl.hexmaster.leakout.core.level

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class LevelWriterTest {
    private fun tree(level: Level): JsonObject = Json.parseToJsonElement(LevelWriter.write(level)).jsonObject

    private fun reparse(level: Level): Level =
        assertIs<ParseResult.Success>(LevelParser.parse(LevelWriter.write(level))).level

    private val minimal = TestLevels.valid(blocked = emptyList())

    @Test
    fun `minimal level has only required keys and the required flow keys`() {
        val json = tree(minimal)
        assertEquals(listOf("id", "name", "grid", "start", "finish", "blocked", "flow", "stars"), json.keys.toList())
        assertEquals(listOf("initialDelayMs", "cellsPerSecond"), json.getValue("flow").jsonObject.keys.toList())
    }

    @Test
    fun `a single changed weight is written alone`() {
        val json = tree(minimal.copy(stack = StackSettings(weights = TestLevels.weights(PipeSection.X to 1))))
        val stack = json.getValue("stack").jsonObject
        assertEquals(listOf("weights"), stack.keys.toList())
        assertEquals(mapOf("X" to "1"), stack.getValue("weights").jsonObject.mapValues { it.value.jsonPrimitive.content })
    }

    @Test
    fun `empty blocked list is written as an empty array`() {
        assertEquals(JsonArray(emptyList()), tree(minimal).getValue("blocked"))
        assertTrue(LevelWriter.write(minimal).contains("\"blocked\": []"))
    }

    @Test
    fun `blocked cells keep their order`() {
        val blocked = listOf(Position(5, 1), Position(1, 3), Position(2, 0))
        val written = tree(minimal.copy(blocked = blocked)).getValue("blocked").jsonArray.map {
            Position(it.jsonObject.getValue("x").jsonPrimitive.content.toInt(), it.jsonObject.getValue("y").jsonPrimitive.content.toInt())
        }
        assertEquals(blocked, written)
    }

    @Test
    fun `output uses two-space indentation and ends with a newline`() {
        val text = LevelWriter.write(minimal)
        assertTrue(text.endsWith("}\n"), text)
        assertTrue(text.lines()[1].startsWith("  \"id\""), text)
    }

    @Test
    fun `rules are written only when they differ from the defaults`() {
        assertEquals(
            listOf("replacePenaltyMs"),
            tree(minimal.copy(rules = PlacementRules(replacePenaltyMs = 750))).getValue("rules").jsonObject.keys.toList(),
        )
    }

    @Test
    fun `a level with every optional field non-default round-trips`() {
        val level = TestLevels.valid(
            flow = FlowSettings(12_000, 0.6, reservoirSpeedFactor = 0.25, speedRampPerCell = 0.02),
            stack = StackSettings(visibleSize = 4, weights = TestLevels.weights(PipeSection.H to 4, PipeSection.X to 1, PipeSection.RES to 2)),
            rules = PlacementRules(allowReplace = false, replacePenaltyMs = 750),
            blocked = listOf(Position(3, 4), Position(2, 2)),
            mandatory = listOf(MandatorySection(Position(3, 2), PipeSection.X)),
        )
        assertEquals(level, reparse(level))
    }

    @Test
    fun `an invalid level still writes and parses back`() {
        val level = minimal.copy(start = Endpoint(Position(0, 1), Side.LEFT), stars = StarBenchmarks(3, 2, 1))
        assertTrue(LevelValidator.validate(level).errors.isNotEmpty())
        assertEquals(level, reparse(level))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("levelFileNames")
    fun `bundled level round-trips through write and parse`(fileName: String) {
        val first = assertIs<ParseResult.Success>(LevelParser.parse(File(levelsDir, fileName).readText())).level
        assertEquals(first, reparse(first))
    }

    companion object {
        private val levelsDir: File by lazy {
            File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        }

        @JvmStatic
        fun levelFileNames(): List<String> =
            levelsDir.listFiles { file -> file.name.matches(Regex("""level_\d{3}\.json""")) }.orEmpty().map { it.name }.sorted()
    }
}
