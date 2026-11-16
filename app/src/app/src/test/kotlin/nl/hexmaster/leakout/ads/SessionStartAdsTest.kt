package nl.hexmaster.leakout.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/** A scripted [AdProvider]: it has an ad ready (or not), and the "player" closes the ad when the test says so. */
private class FakeAdProvider(var ready: Boolean) : AdProvider {
    var shown = 0
    var preloads = 0
    private var pending: (() -> Unit)? = null

    override val isReady: Boolean get() = ready

    override fun preload() {
        preloads++
    }

    override fun showIfReady(onFinished: () -> Unit) {
        if (!ready) {
            onFinished()
            return
        }
        shown++
        ready = false
        pending = onFinished
    }

    fun playerClosesAd() {
        pending?.invoke()
        pending = null
    }
}

class SessionStartAdsTest {
    @Test
    fun `a menu start with an ad ready shows it and opens the level only when it is closed`() {
        val provider = FakeAdProvider(ready = true)
        var opened = 0

        SessionStartAds(provider).beforeMenuStart { opened++ }

        assertEquals(1, provider.shown)
        assertEquals("the level waits for the ad", 0, opened)
        provider.playerClosesAd()
        assertEquals(1, opened)
    }

    @Test
    fun `a menu start without a ready ad opens the level at once and tries to load the next ad`() {
        val provider = FakeAdProvider(ready = false)
        var opened = 0

        SessionStartAds(provider).beforeMenuStart { opened++ }

        assertEquals(1, opened)
        assertEquals(0, provider.shown)
        assertEquals(1, provider.preloads)
    }

    @Test
    fun `starting from the menu again after the first ad shows a new one`() {
        val provider = FakeAdProvider(ready = true)
        val ads = SessionStartAds(provider)
        ads.beforeMenuStart { }
        provider.playerClosesAd()

        provider.ready = true // the next ad finished preloading
        ads.beforeMenuStart { }

        assertEquals(2, provider.shown)
    }

    @Test
    fun `NoAds opens the level straight away`() {
        var opened = 0
        SessionStartAds(NoAds).beforeMenuStart { opened++ }
        assertEquals(1, opened)
    }
}
