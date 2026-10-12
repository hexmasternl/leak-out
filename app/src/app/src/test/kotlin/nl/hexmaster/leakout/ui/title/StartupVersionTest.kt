package nl.hexmaster.leakout.ui.title

import nl.hexmaster.leakout.ui.settings.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupVersionTest {

    @Test
    fun `formats the version name and code`() {
        assertEquals("v1.4.0 (57)", StartupVersion.format(AppInfo(versionName = "1.4.0", versionCode = 57)))
    }

    @Test
    fun `keeps a long version code whole`() {
        assertEquals("v2.0.1 (3000000000)", StartupVersion.format(AppInfo(versionName = "2.0.1", versionCode = 3_000_000_000)))
    }

    @Test
    fun `holds 5 s and fades over 300 ms, or 160 ms with reduced motion`() {
        assertEquals(5_000L, StartupVersion.HOLD_MS)
        assertEquals(300, StartupVersion.FADE_MS)
        assertEquals(160, StartupVersion.REDUCED_FADE_MS)
    }
}
