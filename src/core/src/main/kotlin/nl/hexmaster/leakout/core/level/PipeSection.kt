package nl.hexmaster.leakout.core.level

/**
 * The pipe sections that can appear on the stack, by the ids used in level files. Straight and
 * corner sections are [alwaysOnStack]: every level must be able to draw them.
 */
enum class PipeSection(
    val alwaysOnStack: Boolean,
    val openSides: Set<Side>,
) {
    H(alwaysOnStack = true, openSides = setOf(Side.LEFT, Side.RIGHT)),
    V(alwaysOnStack = true, openSides = setOf(Side.TOP, Side.BOTTOM)),
    LT(alwaysOnStack = true, openSides = setOf(Side.LEFT, Side.TOP)),
    LB(alwaysOnStack = true, openSides = setOf(Side.LEFT, Side.BOTTOM)),
    RT(alwaysOnStack = true, openSides = setOf(Side.RIGHT, Side.TOP)),
    RB(alwaysOnStack = true, openSides = setOf(Side.RIGHT, Side.BOTTOM)),
    X(alwaysOnStack = false, openSides = Side.entries.toSet()),
    RES(alwaysOnStack = false, openSides = setOf(Side.LEFT, Side.RIGHT)),
}

/**
 * The side liquid leaves this section through when it enters through [entry], or `null` when
 * [entry] has no opening. A cross is passed straight through, so each axis is its own channel; a
 * reservoir only runs between left and right. The route check and the flow both use this rule.
 */
fun PipeSection.exitFor(entry: Side): Side? = when {
    entry !in openSides -> null
    this == PipeSection.X -> entry.opposite()
    else -> (openSides - entry).single()
}
