package nl.hexmaster.leakout.core.level

/**
 * The pipe sections that can appear on the stack, by the ids used in level files. Straight and
 * corner sections are [mandatory]: every level must be able to draw them.
 */
enum class PipeSection(val mandatory: Boolean) {
    H(mandatory = true),
    V(mandatory = true),
    LT(mandatory = true),
    LB(mandatory = true),
    RT(mandatory = true),
    RB(mandatory = true),
    X(mandatory = false),
    RES(mandatory = false),
}
