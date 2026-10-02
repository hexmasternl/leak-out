package nl.hexmaster.leakout

import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsDebugTest {
    @Test
    fun `level designer is on in debug builds`() {
        assertTrue(FeatureFlags.levelDesigner)
    }
}
