package nl.hexmaster.leakout.ads

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import nl.hexmaster.leakout.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** The AdMob wiring in the build and the manifest (openspec/specs/session-start-ads). */
class AdMobConfigTest {
    private val android = "http://schemas.android.com/apk/res/android"
    private val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
    private val leakOutInterstitialId = "ca-app-pub-1310013076234295/8713231863"

    private val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> =
        manifest.getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    @Test
    fun `the ad unit id has the AdMob format, debug uses the test unit and release the Leak Out unit`() {
        assertTrue(Regex("""ca-app-pub-\d{16}/\d{10}""").matches(BuildConfig.ADMOB_INTERSTITIAL_ID))
        if (BuildConfig.DEBUG) assertEquals(testInterstitialId, BuildConfig.ADMOB_INTERSTITIAL_ID)
        // Release builds use the Leak Out unit unless the build environment overrides it.
        if (!BuildConfig.DEBUG && System.getenv("LEAKOUT_ADMOB_INTERSTITIAL_ID").isNullOrBlank()) {
            assertEquals(leakOutInterstitialId, BuildConfig.ADMOB_INTERSTITIAL_ID)
        }
    }

    @Test
    fun `the manifest takes the AdMob app id from the build, so debug and release can differ`() {
        val meta = elements("meta-data").single { it.getAttributeNS(android, "name") == "com.google.android.gms.ads.APPLICATION_ID" }
        assertEquals("\${admobAppId}", meta.getAttributeNS(android, "value"))
    }

    @Test
    fun `the manifest declares the advertising id permission`() {
        val permissions = elements("uses-permission").map { it.getAttributeNS(android, "name") }
        assertTrue("com.google.android.gms.permission.AD_ID" in permissions)
    }
}
