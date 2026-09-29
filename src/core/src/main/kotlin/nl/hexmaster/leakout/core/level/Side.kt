package nl.hexmaster.leakout.core.level

/**
 * A side of a grid cell. [dx] and [dy] are the offset to the neighbouring cell on that side, with
 * `x` growing to the right and `y` growing downward.
 */
enum class Side(val dx: Int, val dy: Int) {
    LEFT(-1, 0),
    RIGHT(1, 0),
    TOP(0, -1),
    BOTTOM(0, 1);

    fun opposite(): Side = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        TOP -> BOTTOM
        BOTTOM -> TOP
    }
}
