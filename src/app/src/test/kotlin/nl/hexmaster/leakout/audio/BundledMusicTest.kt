package nl.hexmaster.leakout.audio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's copy of the music must stay identical to the design source in the repository's `assets/audio/`. */
class BundledMusicTest {
    private val bundledDir = File("src/main/assets/music")
    private val sourceDir = File("../../assets/audio")

    private fun credits(dir: File): Playlist {
        val file = File(dir, "audio_credits.json")
        assertTrue("missing $file", file.isFile)
        val playlist = AudioCreditsParser.parse(file.readText())
        assertNotNull("$file does not parse", playlist)
        return playlist!!
    }

    @Test
    fun `bundled credits equal the design source`() {
        assertEquals(credits(sourceDir).tracks, credits(bundledDir).tracks)
    }

    @Test
    fun `ten tracks in file-number order`() {
        val ids = credits(bundledDir).tracks.map { it.id }
        assertEquals((1..10).map { "bg_music_%04d".format(it) }, ids)
    }

    @Test
    fun `every bundled track exists and matches its source`() {
        for (track in credits(bundledDir).tracks) {
            val bundled = File(bundledDir, track.file)
            val source = File(sourceDir, track.file)
            assertTrue("missing bundled ${track.file}", bundled.isFile)
            assertTrue("missing source ${track.file}", source.isFile)
            assertEquals("size of ${track.file}", source.length(), bundled.length())
        }
    }

    @Test
    fun `no stray files in the music folder`() {
        val expected = credits(bundledDir).tracks.map { it.file }.toSet() + "audio_credits.json"
        assertEquals(expected, bundledDir.list().orEmpty().toSet())
    }
}
