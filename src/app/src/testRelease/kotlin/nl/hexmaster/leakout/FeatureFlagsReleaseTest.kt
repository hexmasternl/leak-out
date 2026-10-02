package nl.hexmaster.leakout

import org.junit.Assert.assertFalse
import org.junit.Test

/** Runs with `:app:testReleaseUnitTest`, against the release build's generated BuildConfig. */
class FeatureFlagsReleaseTest {
    @Test
    fun `level designer is off in release builds`() {
        assertFalse(BuildConfig.LEVEL_DESIGNER)
        assertFalse(FeatureFlags.levelDesigner)
    }
}
