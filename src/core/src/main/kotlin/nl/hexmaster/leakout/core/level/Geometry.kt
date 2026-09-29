package nl.hexmaster.leakout.core.level

/** A cell on the grid: [x] is the zero-based column, [y] the zero-based row from the top. */
data class Position(val x: Int, val y: Int) {
    fun neighbour(side: Side): Position = Position(x + side.dx, y + side.dy)

    override fun toString(): String = "($x, $y)"
}

/** The size of a level's grid in cells. */
data class GridSize(val columns: Int, val rows: Int) {
    operator fun contains(position: Position): Boolean =
        position.x in 0 until columns && position.y in 0 until rows

    override fun toString(): String = "$columns by $rows"
}

/**
 * The start or the finish of a level. [openSide] is the side of the cell with the opening: liquid
 * leaves the start through it and must enter the finish through it.
 */
data class Endpoint(val position: Position, val openSide: Side) {
    /** The neighbouring cell on the open side, which a pipe must occupy to connect this endpoint. */
    val facingCell: Position get() = position.neighbour(openSide)
}
