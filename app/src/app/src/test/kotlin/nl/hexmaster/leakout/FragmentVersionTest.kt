package nl.hexmaster.leakout

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Play services asks for fragment 1.1.0, which the Play Console reports as outdated (issue #88).
 * The app pins a newer fragment library to override it; this test fails if that pin is dropped
 * and the build falls back to 1.1.0.
 */
class FragmentVersionTest {
    @Test
    fun fragmentLibraryIsNewerThanTheOutdatedOnePlayServicesAsksFor() {
        // OnBackStackChangedListener#onBackStackChangeProgressed was added in fragment 1.7.0.
        val listener = Class.forName("androidx.fragment.app.FragmentManager\$OnBackStackChangedListener")
        assertTrue(
            "androidx.fragment:fragment resolves to an outdated version; keep the fragment pin from issue #88",
            listener.methods.any { it.name == "onBackStackChangeProgressed" },
        )
    }
}
