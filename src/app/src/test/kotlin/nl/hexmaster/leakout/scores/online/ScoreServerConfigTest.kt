package nl.hexmaster.leakout.scores.online

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Score posting is on only with an https address and a client id (spec: Score server configuration). */
class ScoreServerConfigTest {
    private val clientId = "201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com"

    @Test
    fun `the default pair is enabled`() {
        assertTrue(ScoreServerConfig("https://api.leak-out.com", clientId).enabled)
    }

    @Test
    fun `a blank address turns posting off`() {
        assertFalse(ScoreServerConfig("", clientId).enabled)
        assertFalse(ScoreServerConfig("   ", clientId).enabled)
        assertFalse(ScoreServerConfig("https://", clientId).enabled)
    }

    @Test
    fun `a blank client id turns posting off`() {
        assertFalse(ScoreServerConfig("https://api.leak-out.com", "").enabled)
        assertFalse(ScoreServerConfig("https://api.leak-out.com", "  ").enabled)
    }

    @Test
    fun `cleartext http is not accepted`() {
        assertFalse(ScoreServerConfig("http://api.leak-out.com", clientId).enabled)
    }

    @Test
    fun `the build's values are read`() {
        // The defaults (or the overrides the build was made with) arrive through BuildConfig.
        val config = ScoreServerConfig.fromBuildConfig()
        assertTrue(config.baseUrl == nl.hexmaster.leakout.BuildConfig.SCORE_SERVER_URL)
        assertTrue(config.webClientId == nl.hexmaster.leakout.BuildConfig.SCORE_SERVER_CLIENT_ID)
    }
}
