package nl.hexmaster.leakout.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google's User Messaging Platform (UMP) consent flow (openspec/specs/ad-consent). Ads are requested
 * only after [gather] reports that the player's choices allow it, and the Settings screen offers the
 * form again through [showPrivacyOptions] while [privacyOptionsRequired] is true.
 */
class ConsentManager(activity: Activity) {
    private val information: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** The Settings screen shows its "Privacy options" row while this is true. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    /** Ads may be requested right now. */
    val canRequestAds: Boolean get() = information.canRequestAds()

    /**
     * Updates the consent state, shows the consent form where the region requires it, then calls
     * [onAllowed] once, if ads may be requested. When consent was given on an earlier run this calls
     * [onAllowed] at once, without waiting for the network.
     */
    fun gather(activity: Activity, onAllowed: () -> Unit) {
        var notified = false
        val notifyIfAllowed = {
            _privacyOptionsRequired.value =
                information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            if (!notified && information.canRequestAds()) {
                notified = true
                onAllowed()
            }
        }
        information.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { notifyIfAllowed() }
            },
            { notifyIfAllowed() },
        )
        // A returning player's stored consent already allows ads; do not wait for the update.
        notifyIfAllowed()
    }

    /** Reopens the consent form so the player can change their choice. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}
