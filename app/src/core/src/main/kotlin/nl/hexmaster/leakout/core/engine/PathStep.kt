package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/** One exit of the liquid from the [section] on the tile [at], as [GameState.flowPath] records it. Mandatory sections included. */
data class PathStep(val at: Position, val section: PipeSection)
