package nl.hexmaster.leakout.core.ads

/**
 * Decides whether an interstitial ad is shown when a level starts (openspec/specs/session-start-ads).
 *
 * An ad belongs to the start of a session: a level or the Daily Pipe opening from the menu. Moving on
 * to the next level, or restarting, happens inside the game and never shows one. Having no ad ready
 * means no ad: the player is never made to wait for one.
 */
object AdSessionGate {
    /**
     * @param fromMenu the level opens from a screen other than a level ("Continue", the level library,
     *   the Daily Pipe), so it starts a new session.
     * @param adReady an ad has been loaded and can be shown at once.
     */
    fun shouldShow(fromMenu: Boolean, adReady: Boolean): Boolean = fromMenu && adReady
}
