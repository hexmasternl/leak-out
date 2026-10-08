package nl.hexmaster.leakout.core.level

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Turns a [Level] back into level-format JSON that [LevelParser] reads as the same level. Required
 * fields are always written; an optional field only when it differs from [LevelDefaults], so the
 * output looks like a hand-written level file. Keys follow the order of `docs/level-format.md`.
 * It does not validate: an invalid level is written as it is.
 */
object LevelWriter {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun write(level: Level): String = json.encodeToString(JsonElement.serializer(), toJson(level)) + "\n"

    private fun toJson(level: Level): JsonObject = buildJsonObject {
        put("id", level.id)
        put("name", level.name)
        put("grid", buildJsonObject {
            put("columns", level.grid.columns)
            put("rows", level.grid.rows)
        })
        put("start", endpoint(level.start))
        put("finish", endpoint(level.finish))
        put("blocked", buildJsonArray { level.blocked.forEach { add(cell(it)) } })
        put("flow", flow(level.flow))
        stack(level.stack)?.let { put("stack", it) }
        rules(level.rules)?.let { put("rules", it) }
        put("stars", buildJsonObject {
            put("one", level.stars.one)
            put("two", level.stars.two)
            put("three", level.stars.three)
        })
        if (level.mandatory.isNotEmpty()) {
            put("mandatory", buildJsonArray {
                level.mandatory.forEach { (position, section) ->
                    add(buildJsonObject {
                        put("x", position.x)
                        put("y", position.y)
                        put("section", section.name)
                    })
                }
            })
        }
        if (level.portals.isNotEmpty()) {
            put("portals", buildJsonArray {
                level.portals.forEach { pair ->
                    add(buildJsonObject {
                        put("a", endpoint(pair.a))
                        put("b", endpoint(pair.b))
                    })
                }
            })
        }
    }

    private fun endpoint(endpoint: Endpoint): JsonObject = buildJsonObject {
        put("x", endpoint.position.x)
        put("y", endpoint.position.y)
        put("openSide", endpoint.openSide.name)
    }

    private fun cell(position: Position): JsonObject = buildJsonObject {
        put("x", position.x)
        put("y", position.y)
    }

    private fun flow(flow: FlowSettings): JsonObject = buildJsonObject {
        put("initialDelayMs", flow.initialDelayMs)
        put("cellsPerSecond", flow.cellsPerSecond)
        putUnlessDefault("reservoirSpeedFactor", flow.reservoirSpeedFactor, LevelDefaults.RESERVOIR_SPEED_FACTOR)
        putUnlessDefault("speedRampPerCell", flow.speedRampPerCell, LevelDefaults.SPEED_RAMP_PER_CELL)
    }

    /** The stack object, or `null` when it holds only defaults. */
    private fun stack(stack: StackSettings): JsonObject? {
        val weights = buildJsonObject {
            PipeSection.entries.forEach { section ->
                putUnlessDefault(section.name, stack.weightOf(section), LevelDefaults.STACK_WEIGHTS.getValue(section))
            }
        }
        return buildJsonObject {
            putUnlessDefault("visibleSize", stack.visibleSize, LevelDefaults.VISIBLE_STACK_SIZE)
            if (weights.isNotEmpty()) put("weights", weights)
        }.takeIf { it.isNotEmpty() }
    }

    /** The rules object, or `null` when it holds only defaults. */
    private fun rules(rules: PlacementRules): JsonObject? = buildJsonObject {
        putUnlessDefault("allowReplace", rules.allowReplace, LevelDefaults.ALLOW_REPLACE)
        putUnlessDefault("replacePenaltyMs", rules.replacePenaltyMs, LevelDefaults.REPLACE_PENALTY_MS)
    }.takeIf { it.isNotEmpty() }

    private fun JsonObjectBuilder.putUnlessDefault(key: String, value: Number, default: Number) {
        if (value != default) put(key, value)
    }

    private fun JsonObjectBuilder.putUnlessDefault(key: String, value: Boolean, default: Boolean) {
        if (value != default) put(key, value)
    }
}
