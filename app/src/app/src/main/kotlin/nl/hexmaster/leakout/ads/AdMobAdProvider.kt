package nl.hexmaster.leakout.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

/**
 * The AdMob interstitial behind [AdProvider]. Every method runs on the main thread, as the SDK needs.
 *
 * Ads are only requested once [start] was called, which the app does after the consent flow allowed
 * it. The activity that shows the ad is attached with [attach] and held weakly, so it can be recreated.
 */
class AdMobAdProvider(
    private val context: Context,
    private val adUnitId: String,
) : AdProvider {
    private var allowed = false
    private var loading = false
    private var showing = false
    private var ad: InterstitialAd? = null
    private var activity: WeakReference<Activity>? = null

    override val isReady: Boolean get() = ad != null && !showing

    /** The activity that shows the ads; call from onCreate and [detach] from onDestroy. */
    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (this.activity?.get() === activity) this.activity = null
    }

    /** Ads may be requested: initialises the SDK once (call off the main thread) and is followed by [preload]. */
    fun initializeSdk() {
        MobileAds.initialize(context)
    }

    /** Lets [preload] request ads. Called when consent allows ads and the SDK is initialised. */
    fun start() {
        allowed = true
        preload()
    }

    override fun preload() {
        if (!allowed || loading || ad != null) return
        loading = true
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    ad = null
                }
            },
        )
    }

    override fun showIfReady(onFinished: () -> Unit) {
        if (showing) return
        val loaded = ad
        val host = activity?.get()
        if (loaded == null || host == null) {
            onFinished()
            preload()
            return
        }
        showing = true
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish(onFinished)

            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish(onFinished)
        }
        loaded.show(host)
    }

    private fun finish(onFinished: () -> Unit) {
        showing = false
        ad = null
        onFinished()
        preload()
    }
}
