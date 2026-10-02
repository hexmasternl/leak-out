package nl.hexmaster.leakout.core.level

import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A problem with the shape of a level file. [field] is the dotted path, or null for malformed JSON. */
data class ParseError(val field: String?, val message: String)

sealed interface ParseResult {
    data class Success(val level: Level) : ParseResult
    data class Failure(val error: ParseError) : ParseResult
}

/**
 * Turns level JSON into a [Level]. Checks shape and types only: required fields, types, unknown
 * keys and side names. Whether the values make a playable level is [LevelValidator]'s job.
 */
object LevelParser {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    private val pathPattern = Regex("""at path:? \$([^\s]*)""")
    private val unknownKeyPattern = Regex("""unknown key '([^']*)'""")
    private val unknownEnumPattern = Regex("""does not contain element with name '([^']*)'""")
    private val offsetPrefix = Regex("""^Unexpected JSON token at offset \d+: """)

    fun parse(text: String): ParseResult {
        val tree = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            return failure(null, "The level is not valid JSON: ${firstLine(e)}")
        }
        quotedBoolean(tree, LevelDto.serializer().descriptor, path = "")?.let { return ParseResult.Failure(it) }
        return try {
            ParseResult.Success(json.decodeFromString(LevelDto.serializer(), text).toLevel())
        } catch (e: MissingFieldException) {
            val fields = e.missingFields.map { join(pathOf(e), it) }
            failure(fields.first(), "Missing required field ${fields.joinToString { "'$it'" }}")
        } catch (e: SerializationException) {
            describe(e)
        }
    }

    private fun describe(e: SerializationException): ParseResult.Failure {
        val message = e.message.orEmpty()
        val path = pathOf(e)
        unknownKeyPattern.find(message)?.let {
            val field = join(path, it.groupValues[1])
            return failure(field, "Unknown field '$field'")
        }
        unknownEnumPattern.find(message)?.let {
            val allowed = if (path.endsWith(".section")) PipeSection.entries.joinToString() else Side.entries.joinToString()
            return failure(path, "Invalid value '${it.groupValues[1]}' for '$path'; expected one of $allowed")
        }
        val detail = firstLine(e).replace(offsetPrefix, "").replace(pathPattern, "").trim()
        return failure(path.ifEmpty { null }, if (path.isEmpty()) detail else "Invalid value for '$path': $detail")
    }

    // The default decoder accepts "true" for a boolean; level files must use the literal.
    private fun quotedBoolean(element: JsonElement, descriptor: SerialDescriptor, path: String): ParseError? {
        when (descriptor.kind) {
            PrimitiveKind.BOOLEAN ->
                if (element is JsonPrimitive && element.isString) {
                    return ParseError(path, "Invalid value for '$path': expected true or false, but had $element")
                }
            StructureKind.CLASS -> if (element is JsonObject) {
                for (i in 0 until descriptor.elementsCount) {
                    val name = descriptor.getElementName(i)
                    val child = element[name] ?: continue
                    quotedBoolean(child, descriptor.getElementDescriptor(i), join(path, name))?.let { return it }
                }
            }
            StructureKind.LIST -> if (element is JsonArray) {
                element.forEachIndexed { index, child ->
                    quotedBoolean(child, descriptor.getElementDescriptor(0), "$path[$index]")?.let { return it }
                }
            }
            else -> Unit
        }
        return null
    }

    /** The dotted path of the element an exception concerns: `$.flow` becomes `flow`, `$` becomes empty. */
    private fun pathOf(e: SerializationException): String =
        pathPattern.find(e.message.orEmpty())?.groupValues?.get(1)?.removePrefix(".").orEmpty()

    private fun join(path: String, name: String): String = if (path.isEmpty()) name else "$path.$name"

    private fun firstLine(e: Exception): String = e.message.orEmpty().lineSequence().first()

    private fun failure(field: String?, message: String) = ParseResult.Failure(ParseError(field, message))
}
