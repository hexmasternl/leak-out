package nl.hexmaster.leakout.core.update

/** What a Google Play update check means for the player (openspec/specs/app-updates). */
enum class UpdateStatus {
    /** No newer version, or the check failed or could not run. */
    None,

    /** A newer version the player may install, but does not have to. */
    Optional,

    /** A newer version the player must install before starting another level. */
    Required,
}

/** The lowest in-app update priority that makes an update required. The release pipeline sets 5 or 0. */
const val REQUIRED_UPDATE_PRIORITY = 4

/**
 * Classifies an update check. Play reports priorities 0 to 5; anything at or above
 * [REQUIRED_UPDATE_PRIORITY] is required and anything below it, including an out-of-range negative
 * value, is optional.
 */
fun classifyUpdate(available: Boolean, priority: Int): UpdateStatus = when {
    !available -> UpdateStatus.None
    priority >= REQUIRED_UPDATE_PRIORITY -> UpdateStatus.Required
    else -> UpdateStatus.Optional
}
