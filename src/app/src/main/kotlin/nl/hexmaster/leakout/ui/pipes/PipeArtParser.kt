package nl.hexmaster.leakout.ui.pipes

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import nl.hexmaster.leakout.core.level.Side
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Reads a pipe asset into a [PipeArt]. Only the SVG subset in `docs/pipe-assets.md` is supported;
 * anything else fails with [UnsupportedSvgException] so an edit that the renderer cannot draw is
 * caught by the asset tests instead of silently showing missing art.
 *
 * Elements and attributes in other namespaces (the extras SVG editors add) and `title`, `desc`
 * and `metadata` are ignored. Pure Kotlin on the platform XML parser, so it runs on the JVM too.
 */
object PipeArtParser {
    private const val SVG_NS = "http://www.w3.org/2000/svg"

    private val PRESENTATION = setOf(
        "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
        "opacity", "fill-opacity", "stroke-opacity",
    )
    private val ALLOWED = mapOf(
        "svg" to setOf("viewBox", "width", "height", "version"),
        "g" to PRESENTATION,
        "defs" to emptySet(),
        "path" to PRESENTATION + "d",
        "rect" to PRESENTATION + setOf("x", "y", "width", "height", "rx"),
        "circle" to PRESENTATION + setOf("cx", "cy", "r"),
        "linearGradient" to setOf("x1", "y1", "x2", "y2", "gradientUnits"),
        "stop" to setOf("offset", "stop-color", "stop-opacity"),
    )
    private val IGNORED = setOf("title", "desc", "metadata")
    private val SHAPES = setOf("path", "rect", "circle")

    fun parse(input: InputStream): PipeArt {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val root = factory.newDocumentBuilder().parse(input).documentElement
        if (root.namespaceURI != SVG_NS || root.localName != "svg") {
            throw UnsupportedSvgException("The root element must be an SVG <svg>")
        }
        checkAttributes(root)
        if (root.getAttribute("viewBox").split(Regex("[\\s,]+")).map { it.toFloatOrNull() } !=
            listOf(0f, 0f, 100f, 100f)
        ) {
            throw UnsupportedSvgException("The view box must be \"0 0 100 100\"")
        }

        val gradients = mutableMapOf<String, SvgLinearGradient>()
        collectGradients(root, gradients)

        var under: List<SvgShape>? = null
        var over: List<SvgShape>? = null
        val channels = mutableListOf<ArtChannel>()
        var tank: TankCircle? = null
        val bridges = mutableListOf<Bridge>()

        for (child in svgChildren(root)) {
            checkAttributes(child)
            val id = child.getAttribute("id")
            when {
                child.localName == "defs" -> Unit
                child.localName == "g" && id == "under" ->
                    under = shapes(child, SvgStyle().inherit(child, gradients), gradients)
                child.localName == "g" && id == "over" ->
                    over = shapes(child, SvgStyle().inherit(child, gradients), gradients)
                child.localName == "path" && id.startsWith("channel-") -> channels += ArtChannel(
                    id = id,
                    from = side(child, "data-from"),
                    to = side(child, "data-to"),
                    pathData = child.getAttribute("d").also {
                        if (it.isBlank()) throw UnsupportedSvgException("Channel '$id' has no path data")
                    },
                )
                child.localName == "circle" && id == "tank" -> {
                    if (tank != null) throw UnsupportedSvgException("More than one tank")
                    tank = TankCircle(child.number("cx"), child.number("cy"), child.number("r"))
                }
                child.localName == "rect" && id == "bridge" -> bridges += Bridge(
                    hides = child.getAttribute("data-hides"),
                    x = child.number("x"),
                    y = child.number("y"),
                    width = child.number("width"),
                    height = child.number("height"),
                )
                else -> throw UnsupportedSvgException(
                    "<${child.localName}${if (id.isNotEmpty()) " id=\"$id\"" else ""}> must be inside the " +
                        "\"under\" or \"over\" group",
                )
            }
        }

        if (under == null) throw UnsupportedSvgException("Missing the group with id \"under\"")
        if (over == null) throw UnsupportedSvgException("Missing the group with id \"over\"")
        if (channels.isEmpty()) throw UnsupportedSvgException("No channel-* path")
        if (channels.map { it.id }.toSet().size != channels.size) {
            throw UnsupportedSvgException("Duplicate channel ids")
        }
        for (bridge in bridges) {
            if (channels.none { it.id == bridge.hides }) {
                throw UnsupportedSvgException("Bridge hides unknown channel '${bridge.hides}'")
            }
        }
        return PipeArt(under, over, channels, tank, bridges)
    }

    private fun svgChildren(parent: Element): List<Element> {
        val result = mutableListOf<Element>()
        val nodes = parent.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element
            if (element.namespaceURI != SVG_NS) continue
            if (element.localName in IGNORED) continue
            if (element.localName !in ALLOWED) {
                throw UnsupportedSvgException("Unsupported SVG element <${element.localName}>")
            }
            result += element
        }
        return result
    }

    private fun checkAttributes(element: Element) {
        val allowed = ALLOWED.getValue(element.localName)
        val attributes = element.attributes
        for (i in 0 until attributes.length) {
            val attribute = attributes.item(i)
            // Namespaced attributes (xmlns, xml:space, inkscape:*, ...) are editor data, not art.
            if (attribute.namespaceURI != null) continue
            val name = attribute.localName ?: attribute.nodeName
            if (name == "id" || name.startsWith("data-") || name in allowed) continue
            throw UnsupportedSvgException("Unsupported attribute '$name' on <${element.localName}>")
        }
    }

    private fun collectGradients(parent: Element, into: MutableMap<String, SvgLinearGradient>) {
        for (child in svgChildren(parent)) {
            if (child.localName == "linearGradient") {
                checkAttributes(child)
                val id = child.getAttribute("id")
                if (id.isEmpty()) throw UnsupportedSvgException("A gradient needs an id")
                if (child.getAttribute("gradientUnits") != "userSpaceOnUse") {
                    throw UnsupportedSvgException("Gradient '$id' must use gradientUnits=\"userSpaceOnUse\"")
                }
                val stops = svgChildren(child).map { stop ->
                    if (stop.localName != "stop") {
                        throw UnsupportedSvgException("Unsupported SVG element <${stop.localName}> in a gradient")
                    }
                    checkAttributes(stop)
                    GradientStop(
                        offset = stop.number("offset"),
                        rgb = parseColour(stop.getAttribute("stop-color")),
                        opacity = stop.optionalNumber("stop-opacity") ?: 1f,
                    )
                }
                if (stops.isEmpty()) throw UnsupportedSvgException("Gradient '$id' has no stops")
                into[id] = SvgLinearGradient(
                    id, child.number("x1"), child.number("y1"), child.number("x2"), child.number("y2"), stops,
                )
            } else {
                collectGradients(child, into)
            }
        }
    }

    private fun shapes(group: Element, style: SvgStyle, gradients: Map<String, SvgLinearGradient>): List<SvgShape> {
        val result = mutableListOf<SvgShape>()
        for (child in svgChildren(group)) {
            checkAttributes(child)
            val own = style.inherit(child, gradients)
            result += when (child.localName) {
                "g" -> shapes(child, own, gradients)
                "path" -> listOf(SvgShape.PathShape(child.getAttribute("d"), own))
                "rect" -> listOf(
                    SvgShape.RectShape(
                        child.number("x"), child.number("y"), child.number("width"), child.number("height"),
                        child.optionalNumber("rx") ?: 0f, own,
                    ),
                )
                "circle" -> listOf(SvgShape.CircleShape(child.number("cx"), child.number("cy"), child.number("r"), own))
                else -> throw UnsupportedSvgException("Unsupported SVG element <${child.localName}> in a group")
            }
        }
        return result
    }

    /** Applies the element's own presentation attributes on top of the inherited [this]. */
    private fun SvgStyle.inherit(element: Element, gradients: Map<String, SvgLinearGradient>): SvgStyle {
        fun attr(name: String): String? = element.getAttribute(name).takeIf { it.isNotEmpty() }
        return copy(
            // "none" is a value (no paint), so only a missing attribute inherits.
            fill = attr("fill").let { if (it == null) fill else paint(it, gradients) },
            stroke = attr("stroke").let { if (it == null) stroke else paint(it, gradients) },
            strokeWidth = element.optionalNumber("stroke-width") ?: strokeWidth,
            lineCap = attr("stroke-linecap")?.let { enumValue<SvgLineCap>(it, "stroke-linecap") } ?: lineCap,
            lineJoin = attr("stroke-linejoin")?.let { enumValue<SvgLineJoin>(it, "stroke-linejoin") } ?: lineJoin,
            // Group opacity is multiplied into the children; the assets never overlap shapes inside
            // a translucent group, so this matches what an SVG viewer shows.
            opacity = opacity * (element.optionalNumber("opacity") ?: 1f),
            fillOpacity = element.optionalNumber("fill-opacity") ?: fillOpacity,
            strokeOpacity = element.optionalNumber("stroke-opacity") ?: strokeOpacity,
        )
    }

    private fun paint(value: String, gradients: Map<String, SvgLinearGradient>): SvgPaint? {
        if (value == "none") return null
        val url = Regex("""url\(#([^)]+)\)""").matchEntire(value.trim())
        if (url != null) {
            val id = url.groupValues[1]
            return SvgPaint.Gradient(gradients[id] ?: throw UnsupportedSvgException("Unknown gradient '$id'"))
        }
        return SvgPaint.Solid(parseColour(value))
    }

    /** Parses `#RRGGBB` or `#RGB` into `0xRRGGBB`. Named colours are not supported. */
    fun parseColour(value: String): Int {
        val hex = value.trim().removePrefix("#")
        if (!value.trim().startsWith("#") || hex.any { it.digitToIntOrNull(16) == null }) {
            throw UnsupportedSvgException("Unsupported colour '$value', use #RRGGBB")
        }
        return when (hex.length) {
            6 -> hex.toInt(16)
            3 -> hex.map { "$it$it" }.joinToString("").toInt(16)
            else -> throw UnsupportedSvgException("Unsupported colour '$value', use #RRGGBB")
        }
    }

    private inline fun <reified E : Enum<E>> enumValue(value: String, attribute: String): E =
        enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw UnsupportedSvgException("Unsupported $attribute '$value'")

    private fun side(element: Element, attribute: String): Side {
        val value = element.getAttribute(attribute)
        return Side.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw UnsupportedSvgException(
                "Channel '${element.getAttribute("id")}' has $attribute=\"$value\"; use left, right, top or bottom",
            )
    }

    private fun Element.optionalNumber(name: String): Float? {
        val value = getAttribute(name)
        if (value.isEmpty()) return null
        return value.toFloatOrNull() ?: throw UnsupportedSvgException("'$name' on <$localName> is not a number: '$value'")
    }

    private fun Element.number(name: String): Float =
        optionalNumber(name) ?: throw UnsupportedSvgException("<$localName> needs '$name'")
}
