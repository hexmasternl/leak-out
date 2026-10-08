package nl.hexmaster.leakout.core.update

/**
 * Decides what the player sees about app updates (openspec/specs/app-updates), from the latest
 * update check and whether a level is being played. Immutable: every event returns a new gate.
 *
 * "In a level" covers the whole game screen, including its pause, level complete and level lost
 * dialogs, so a required update found mid-level waits until the player leaves the level, and an
 * attempt to start another level from there goes to the blocking screen through [mayStartLevel].
 */
data class UpdateGate(
    val status: UpdateStatus = UpdateStatus.None,
    val inLevel: Boolean = false,
    val optionalOffer: OptionalOffer = OptionalOffer.AwaitingStartCheck,
    val downloaded: Boolean = false,
    val installPromptDismissed: Boolean = false,
) {
    /** The optional update prompt is offered at most once per process, after the check at start. */
    enum class OptionalOffer { AwaitingStartCheck, Pending, Done }

    /** A level may start unless a required update is known and not installed. */
    fun mayStartLevel(): Boolean = status != UpdateStatus.Required

    /** The blocking update screen replaces every screen outside a level while a required update is known. */
    fun showBlockingScreen(): Boolean = status == UpdateStatus.Required && !inLevel

    /** The dismissible optional update prompt, shown outside a level once per process. */
    fun showOptionalPrompt(): Boolean =
        status == UpdateStatus.Optional && optionalOffer == OptionalOffer.Pending && !inLevel && !downloaded

    /** The "restart to install" prompt for a downloaded optional update, never during a level. */
    fun showInstallPrompt(): Boolean =
        downloaded && !installPromptDismissed && !inLevel && status != UpdateStatus.Required

    /**
     * The result of an update check. A failed check arrives as [UpdateStatus.None]. Only the first
     * check of the process can make the optional prompt pending.
     */
    fun checked(result: UpdateStatus): UpdateGate = copy(
        status = result,
        optionalOffer = when (optionalOffer) {
            OptionalOffer.AwaitingStartCheck ->
                if (result == UpdateStatus.Optional) OptionalOffer.Pending else OptionalOffer.Done
            else -> optionalOffer
        },
    )

    fun levelEntered(): UpdateGate = copy(inLevel = true)

    fun levelLeft(): UpdateGate = copy(inLevel = false)

    /** The player accepted or dismissed the optional prompt; it is not offered again in this process. */
    fun optionalPromptClosed(): UpdateGate = copy(optionalOffer = OptionalOffer.Done)

    /** An accepted optional update finished downloading and waits for the player's go-ahead to install. */
    fun optionalDownloaded(): UpdateGate = copy(downloaded = true, optionalOffer = OptionalOffer.Done)

    fun installPromptClosed(): UpdateGate = copy(installPromptDismissed = true)
}
