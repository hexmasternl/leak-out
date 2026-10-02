package nl.hexmaster.leakout.core.level

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LevelParserTest {
    private fun parsed(text: String): Level = assertIs<ParseResult.Success>(LevelParser.parse(text)).level

    private fun failed(text: String): ParseError = assertIs<ParseResult.Failure>(LevelParser.parse(text)).error

    @Test
    fun `complete level is parsed with every value from the document`() {
        val expected = TestLevels.valid(
            flow = FlowSettings(12_000, 0.6, reservoirSpeedFactor = 0.25, speedRampPerCell = 0.02),
            stack = StackSettings(
                visibleSize = 4,
                weights = mapOf(
                    PipeSection.H to 4, PipeSection.V to 4,
                    PipeSection.LT to 1, PipeSection.LB to 1, PipeSection.RT to 1, PipeSection.RB to 1,
                    PipeSection.X to 1, PipeSection.RES to 2,
                ),
            ),
            rules = PlacementRules(allowReplace = false, replacePenaltyMs = 750),
        )
        assertEquals(expected, parsed(TestLevels.FULL_JSON))
    }

    @Test
    fun `minimal level gets all defaults`() {
        val level = parsed(TestLevels.minimalJson())
        assertEquals(0.3, level.flow.reservoirSpeedFactor)
        assertEquals(0.0, level.flow.speedRampPerCell)
        assertEquals(5, level.stack.visibleSize)
        assertEquals(LevelDefaults.STACK_WEIGHTS, level.stack.weights)
        for (section in PipeSection.entries) {
            assertEquals(if (section.alwaysOnStack) 7 else 0, level.stack.weightOf(section), "$section")
        }
        assertEquals(true, level.rules.allowReplace)
        assertEquals(3_000, level.rules.replacePenaltyMs)
        assertEquals(emptyList(), level.blocked)
    }

    @Test
    fun `single flow field is overridden and the rest keep their defaults`() {
        val level = parsed(TestLevels.minimalJson(extraFlow = """, "reservoirSpeedFactor": 0.5"""))
        assertEquals(0.5, level.flow.reservoirSpeedFactor)
        assertEquals(LevelDefaults.SPEED_RAMP_PER_CELL, level.flow.speedRampPerCell)
        assertEquals(StackSettings(), level.stack)
        assertEquals(PlacementRules(), level.rules)
    }

    @Test
    fun `single stack weight is overridden and the rest keep their defaults`() {
        val level = parsed(TestLevels.minimalJson(extra = """, "stack": { "weights": { "X": 3 } }"""))
        assertEquals(TestLevels.weights(PipeSection.X to 3), level.stack.weights)
        assertEquals(7, level.stack.weightOf(PipeSection.H))
        assertEquals(0, level.stack.weightOf(PipeSection.RES))
        assertEquals(LevelDefaults.VISIBLE_STACK_SIZE, level.stack.visibleSize)
    }

    @Test
    fun `cross and reservoir are not allowed without a stack field`() {
        val level = parsed(TestLevels.minimalJson())
        assertEquals(false, level.isAllowed(PipeSection.X))
        assertEquals(false, level.isAllowed(PipeSection.RES))
        assertTrue(PipeSection.entries.filter { it.alwaysOnStack }.all(level::isAllowed))
    }

    @Test
    fun `replacing can be disabled`() {
        val level = parsed(TestLevels.minimalJson(extra = """, "rules": { "allowReplace": false }"""))
        assertEquals(false, level.rules.allowReplace)
        assertEquals(LevelDefaults.REPLACE_PENALTY_MS, level.rules.replacePenaltyMs)
    }

    @Test
    fun `replace penalty can be overridden`() {
        val level = parsed(TestLevels.minimalJson(extra = """, "rules": { "replacePenaltyMs": 1500 }"""))
        assertEquals(1_500, level.rules.replacePenaltyMs)
        assertEquals(LevelDefaults.ALLOW_REPLACE, level.rules.allowReplace)
    }

    @Test
    fun `name is trimmed`() {
        val level = parsed(TestLevels.minimalJson().replace("\"First Leak\"", "\"  First Crossing \""))
        assertEquals("First Crossing", level.name)
    }

    @Test
    fun `missing required field is named with its path`() {
        val error = failed(TestLevels.minimalJson().replace(""", "cellsPerSecond": 0.5""", ""))
        assertEquals("flow.cellsPerSecond", error.field)
        assertTrue("flow.cellsPerSecond" in error.message)
    }

    @Test
    fun `missing top-level field is named`() {
        val error = failed(TestLevels.minimalJson().replace(""""name": "First Leak",""", ""))
        assertEquals("name", error.field)
    }

    @Test
    fun `missing star threshold is named`() {
        val error = failed(TestLevels.minimalJson().replace(""", "three": 150""", ""))
        assertEquals("stars.three", error.field)
    }

    @Test
    fun `unknown field is named`() {
        val error = failed(TestLevels.minimalJson(extra = """, "flowSpeed": 0.4"""))
        assertEquals("flowSpeed", error.field)
        assertTrue("flowSpeed" in error.message)
    }

    @Test
    fun `unknown nested field is named with its path`() {
        val error = failed(TestLevels.minimalJson(extraFlow = """, "speed": 1"""))
        assertEquals("flow.speed", error.field)
    }

    @Test
    fun `unknown section id in weights is named`() {
        val error = failed(TestLevels.minimalJson(extra = """, "stack": { "weights": { "Z": 1 } }"""))
        assertEquals("stack.weights.Z", error.field)
    }

    @Test
    fun `unknown open side is named`() {
        val error = failed(TestLevels.minimalJson().replace("\"RIGHT\"", "\"UP\""))
        assertEquals("start.openSide", error.field)
        assertTrue("'UP'" in error.message)
    }

    @Test
    fun `old direction key is rejected`() {
        val error = failed(TestLevels.minimalJson().replace("\"openSide\": \"RIGHT\"", "\"direction\": \"RIGHT\""))
        assertEquals("start.direction", error.field)
    }

    @Test
    fun `string replace flag is rejected`() {
        val error = failed(TestLevels.minimalJson(extra = """, "rules": { "allowReplace": "yes" }"""))
        assertEquals("rules.allowReplace", error.field)
    }

    @Test
    fun `quoted boolean replace flag is rejected`() {
        val error = failed(TestLevels.minimalJson(extra = """, "rules": { "allowReplace": "true" }"""))
        assertEquals("rules.allowReplace", error.field)
    }

    @Test
    fun `numeric replace flag is rejected`() {
        val error = failed(TestLevels.minimalJson(extra = """, "rules": { "allowReplace": 1 }"""))
        assertEquals("rules.allowReplace", error.field)
    }

    @Test
    fun `wrong number type is named`() {
        val error = failed(TestLevels.minimalJson().replace("\"id\": 1", "\"id\": 1.5"))
        assertEquals("id", error.field)
    }

    @Test
    fun `missing coordinate in a blocked cell is named with its index`() {
        val error = failed(TestLevels.minimalJson().replace("\"blocked\": []", """"blocked": [ { "x": 1, "y": 1 }, { "x": 2 } ]"""))
        assertEquals("blocked[1].y", error.field)
    }

    @Test
    fun `level without a mandatory field has no mandatory sections`() {
        assertEquals(emptyList(), parsed(TestLevels.minimalJson()).mandatory)
    }

    @Test
    fun `mandatory sections are parsed in authored order`() {
        val level = parsed(
            TestLevels.minimalJson(
                extra = """, "mandatory": [ { "x": 3, "y": 2, "section": "X" }, { "x": 1, "y": 1, "section": "RES" } ]""",
            ),
        )
        assertEquals(
            listOf(
                MandatorySection(Position(3, 2), PipeSection.X),
                MandatorySection(Position(1, 1), PipeSection.RES),
            ),
            level.mandatory,
        )
    }

    @Test
    fun `full example in the level format doc parses and validates`() {
        val doc = File(System.getProperty("leakout.levelFormatDoc")).readText()
        val example = Regex("```jsonc\\R(.*?)```", RegexOption.DOT_MATCHES_ALL).find(doc)?.groupValues?.get(1)
            ?: error("no jsonc example in the level format doc")
        val json = example.lines().joinToString("\n") { it.substringBefore("//") }
        val level = parsed(json)
        assertEquals(emptyList(), LevelValidator.validate(level).errors)
        assertEquals(
            listOf(MandatorySection(Position(3, 2), PipeSection.X), MandatorySection(Position(5, 5), PipeSection.RES)),
            level.mandatory,
        )
    }

    @Test
    fun `missing section in a mandatory entry is named with its index`() {
        val error = failed(TestLevels.minimalJson(extra = """, "mandatory": [ { "x": 3, "y": 2 } ]"""))
        assertEquals("mandatory[0].section", error.field)
    }

    @Test
    fun `unknown section id in a mandatory entry is named`() {
        val error = failed(TestLevels.minimalJson(extra = """, "mandatory": [ { "x": 3, "y": 2, "section": "Z" } ]"""))
        assertEquals("mandatory[0].section", error.field)
        assertTrue("'Z'" in error.message)
        assertTrue("RES" in error.message)
    }

    @Test
    fun `unknown key in a mandatory entry is named`() {
        val error = failed(
            TestLevels.minimalJson(extra = """, "mandatory": [ { "x": 3, "y": 2, "section": "X", "fixed": true } ]"""),
        )
        assertEquals("mandatory[0].fixed", error.field)
    }

    @Test
    fun `malformed JSON is rejected without a field`() {
        val error = failed("""{ "id": """)
        assertNull(error.field)
        assertTrue("not valid JSON" in error.message)
    }

    @Test
    fun `non-object document is rejected`() {
        assertIs<ParseResult.Failure>(LevelParser.parse("[1, 2]"))
    }
}
