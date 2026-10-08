package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.PipeSection

/**
 * How fast liquid moves through a section: `cellsPerSecond × factor` sections per second, where
 * the factor is the reservoir speed factor for [PipeSection.RES] and 1 otherwise. Progress is
 * linear in time; nothing eases it.
 */
object LiquidTiming {
    fun speedFactor(section: PipeSection, reservoirSpeedFactor: Float): Float {
        require(reservoirSpeedFactor > 0f) { "The reservoir speed factor must be positive, was $reservoirSpeedFactor" }
        return if (section == PipeSection.RES) reservoirSpeedFactor else 1f
    }

    /** Progress gained per millisecond. */
    fun progressPerMs(cellsPerSecond: Float, section: PipeSection, reservoirSpeedFactor: Float): Float {
        require(cellsPerSecond > 0f) { "The flow speed must be positive, was $cellsPerSecond" }
        return cellsPerSecond * speedFactor(section, reservoirSpeedFactor) / 1000f
    }

    /** Progress after the liquid has been in the section for [elapsedMs] at a constant speed. */
    fun progressAt(elapsedMs: Float, cellsPerSecond: Float, section: PipeSection, reservoirSpeedFactor: Float): Float =
        (elapsedMs * progressPerMs(cellsPerSecond, section, reservoirSpeedFactor)).coerceIn(0f, 1f)

    /**
     * Moves [progress] on by [dtMs] at the current speed. Integrating frame by frame (instead of
     * `elapsed × speed`) lets the speed change mid-flow without the liquid jumping.
     */
    fun advance(progress: Float, dtMs: Float, cellsPerSecond: Float, section: PipeSection, reservoirSpeedFactor: Float): Float {
        require(dtMs >= 0f) { "Time cannot run backwards, dt was $dtMs" }
        return (progress + dtMs * progressPerMs(cellsPerSecond, section, reservoirSpeedFactor)).coerceIn(0f, 1f)
    }

    /** Milliseconds until [progress] reaches 1 at the current speed. */
    fun millisToFill(progress: Float, cellsPerSecond: Float, section: PipeSection, reservoirSpeedFactor: Float): Float =
        (1f - progress.coerceIn(0f, 1f)) / progressPerMs(cellsPerSecond, section, reservoirSpeedFactor)
}
