package nl.hexmaster.leakout.ads

import nl.hexmaster.leakout.core.ads.AdSessionGate

/**
 * The interstitial ad behind a small interface, so the screens and the tests never touch the AdMob SDK
 * (openspec/specs/session-start-ads).
 */
interface AdProvider {
    /** An ad has been loaded and [showIfReady] shows it at once. */
    val isReady: Boolean

    /** Starts loading the next ad in the background. Does nothing while ads are not allowed or one is loading or ready. */
    fun preload()

    /**
     * Shows the loaded ad, then calls [onFinished] when the player closes it. Calls [onFinished] at once
     * when there is no ad or it fails to show. A second call while an ad is on screen is ignored, so a
     * double tap opens the level once. Always preloads the next ad afterwards.
     */
    fun showIfReady(onFinished: () -> Unit)
}

/** An [AdProvider] that never has an ad: used when ads are off and as the default in previews. */
object NoAds : AdProvider {
    override val isReady: Boolean = false

    override fun preload() = Unit

    override fun showIfReady(onFinished: () -> Unit) = onFinished()
}

/**
 * Runs the start of a session: shows an ad first when [AdSessionGate] says so, then opens the level.
 * Only menu starts go through here; moving to the next level or restarting never does.
 */
class SessionStartAds(private val provider: AdProvider) {
    /** Calls [open] once: after the ad is closed, or straight away when no ad is shown. */
    fun beforeMenuStart(open: () -> Unit) {
        if (AdSessionGate.shouldShow(fromMenu = true, adReady = provider.isReady)) {
            provider.showIfReady(open)
        } else {
            open()
            // Not ready now: try again so the next session start has one.
            provider.preload()
        }
    }
}
