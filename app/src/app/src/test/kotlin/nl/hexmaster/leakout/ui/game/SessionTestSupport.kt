package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/**
 * Makes [section] the stack's next section, as if it had been drawn, and places it on [at] with a
 * place command. Returns whether the engine accepted the placement.
 */
internal fun GameSession.placeSection(section: PipeSection, at: Position): Boolean {
    state = state.copy(stack = state.stack.copy(sections = state.stack.sections.dropLast(1) + section))
    return send(GameCommand.Place(at))
}
