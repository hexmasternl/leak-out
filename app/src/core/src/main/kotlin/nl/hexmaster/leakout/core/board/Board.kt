package nl.hexmaster.leakout.core.board

import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/** What a grid cell holds during play. */
sealed interface Cell {
    data object Empty : Cell
    data object Blocked : Cell
    data class Start(val openSide: Side) : Cell
    data class Finish(val openSide: Side) : Cell

    /**
     * A section on the board. Once [liquidEntered] is true it can never be replaced. A [mandatory]
     * section was put there by the level, never by the player, and can never be replaced at all.
     */
    data class Pipe(val section: PipeSection, val liquidEntered: Boolean = false, val mandatory: Boolean = false) : Cell

    /**
     * One portal of a portal pair, put there by the level. [pair] is the pair's number, starting at
     * 1, which picks its colour and symbol. Liquid goes in and out through [openSide]; [partner] is
     * the cell of the other portal of the pair. A portal is not a section: it scores nothing and is
     * never replaced.
     */
    data class Portal(val pair: Int, val openSide: Side, val partner: Position) : Cell
}

/**
 * The cells of a running attempt. Immutable: every change returns a new board. Only non-empty
 * cells are stored, so a position without an entry (inside or outside the grid) is [Cell.Empty].
 */
class Board(val grid: GridSize, cells: Map<Position, Cell>) {
    private val cells: Map<Position, Cell> = cells.filterValues { it != Cell.Empty }

    operator fun get(position: Position): Cell = cells[position] ?: Cell.Empty

    fun with(position: Position, cell: Cell): Board =
        Board(grid, if (cell == Cell.Empty) cells - position else cells + (position to cell))

    /** Marks the pipe at [position] as entered by liquid. Any other cell is left as it is. */
    fun markLiquidEntered(position: Position): Board {
        val cell = this[position] as? Cell.Pipe ?: return this
        return with(position, cell.copy(liquidEntered = true))
    }

    /** Every placed section, by position. */
    val pipes: Map<Position, Cell.Pipe> get() = cells.filterValues { it is Cell.Pipe }.mapValues { it.value as Cell.Pipe }

    override fun equals(other: Any?): Boolean = other is Board && grid == other.grid && cells == other.cells

    override fun hashCode(): Int = 31 * grid.hashCode() + cells.hashCode()

    override fun toString(): String = "Board($grid, $cells)"

    companion object {
        /**
         * The board a level starts with: its start, finish, blocked cells, unfilled mandatory
         * sections and portals, the rest empty.
         */
        fun from(level: Level): Board = Board(
            level.grid,
            level.blocked.associateWith { Cell.Blocked } +
                level.mandatory.associate { it.position to Cell.Pipe(it.section, mandatory = true) } +
                level.portals.flatMapIndexed { index, pair ->
                    listOf(
                        pair.a.position to Cell.Portal(index + 1, pair.a.openSide, pair.b.position),
                        pair.b.position to Cell.Portal(index + 1, pair.b.openSide, pair.a.position),
                    )
                } +
                (level.start.position to Cell.Start(level.start.openSide)) +
                (level.finish.position to Cell.Finish(level.finish.openSide)),
        )
    }
}
