package nl.hexmaster.leakout.ui.designer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.haptics.Haptics
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.game.GameSession

/**
 * A test play of a design on the normal game screen. It records nothing in the level history, offers
 * no next level and shows no tips, so trying a design never touches the player's progress. Leaving
 * the game calls [onExit], which returns to the designer. Its sounds, the points drop included, are
 * the game screen's own, and so are its haptics.
 */
@Composable
fun TestPlayScreen(
    level: Level,
    onExit: () -> Unit,
    soundEffects: SoundEffects? = null,
    haptics: Haptics? = null,
    session: GameSession = remember(level) { GameSession(level) },
) {
    GameScreen(
        level = level,
        onExit = onExit,
        onNextLevel = null,
        history = null,
        session = session,
        soundEffects = soundEffects,
        haptics = haptics,
        tips = null,
    )
}
