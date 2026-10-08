package nl.hexmaster.leakout.audio

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled tracks on a real [android.media.MediaPlayer]: open, play, pause, resume and complete. */
@RunWith(AndroidJUnit4::class)
class MediaPlayerMusicEngineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var engine: MediaPlayerMusicEngine
    private val events = java.util.Collections.synchronizedList(mutableListOf<String>())

    @Before
    fun createEngine() = onMain {
        engine = MediaPlayerMusicEngine(context.assets)
        engine.listener = object : MusicEngine.Listener {
            override fun onStarted(track: Track) { events += "started ${track.id}" }
            override fun onCompleted(track: Track) { events += "completed ${track.id}" }
            override fun onError(track: Track) { events += "error ${track.id}" }
        }
        // Silent on the device; the player still runs.
        engine.setVolume(0f)
    }

    @After
    fun release() = onMain { engine.release() }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun waitFor(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!onMain(condition)) {
            assertTrue("timed out waiting for $what", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(20)
        }
    }

    private val playlist: Playlist by lazy { checkNotNull(BundledMusic.load(context.assets)) { "the bundled credits do not load" } }

    @Test
    fun theBundledPlaylistLoads() {
        assertEquals((1..10).map { "bg_music_%04d".format(it) }, playlist.tracks.map { it.id })
    }

    @Test
    fun playsPausesResumesAndCompletes() {
        val track = playlist.tracks.first()
        onMain { engine.play(track) }
        waitFor("playing") { engine.isPlaying }
        assertEquals(listOf("started ${track.id}"), events.toList())

        onMain { engine.pause() }
        assertFalse(onMain { engine.isPlaying })
        val pausedAt = onMain { engine.positionMs }
        SystemClock.sleep(300)
        assertEquals(pausedAt, onMain { engine.positionMs })

        onMain { engine.resume() }
        waitFor("playing again") { engine.isPlaying }
        assertTrue(onMain { engine.positionMs } >= pausedAt)
        assertEquals("a resume is not a start", listOf("started ${track.id}"), events.toList())

        onMain { engine.seekToEnd(300) }
        waitFor("completion", timeoutMs = 10_000) { "completed ${track.id}" in events }
    }

    @Test
    fun everyBundledTrackOpensAndStarts() {
        for (track in playlist.tracks) {
            onMain { engine.play(track) }
            waitFor("${track.id} playing") { engine.isPlaying }
        }
        assertEquals(playlist.tracks.map { "started ${it.id}" }, events.toList())
    }

    @Test
    fun aMissingFileIsReportedAsAnError() {
        val missing = Track("no_such_track.mp3", "Missing", "Nobody")
        onMain { engine.play(missing) }
        waitFor("error") { "error no_such_track" in events }
        assertFalse(onMain { engine.isPlaying })
    }

    @Test
    fun aTrackPausedWhilePreparingWaitsForResume() {
        val track = playlist.tracks[1]
        onMain {
            engine.play(track)
            engine.pause()
        }
        SystemClock.sleep(1_000)
        assertFalse(onMain { engine.isPlaying })
        assertTrue(events.isEmpty())
        onMain { engine.resume() }
        waitFor("playing") { engine.isPlaying }
        assertEquals(listOf("started ${track.id}"), events.toList())
    }
}
