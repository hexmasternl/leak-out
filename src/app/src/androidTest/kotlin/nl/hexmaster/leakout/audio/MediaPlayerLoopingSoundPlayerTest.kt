package nl.hexmaster.leakout.audio

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled heartbeat on a real [android.media.MediaPlayer]: start, loop, volume, stop and restart. */
@RunWith(AndroidJUnit4::class)
class MediaPlayerLoopingSoundPlayerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var player: MediaPlayerLoopingSoundPlayer

    @Before
    fun createPlayer() = onMain { player = MediaPlayerLoopingSoundPlayer(context.assets) }

    @After
    fun release() = onMain { player.release() }

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

    @Test
    fun startsAndChangesVolumeWhilePlaying() {
        // Silent on the device; the player still runs.
        onMain { player.start(HEARTBEAT, 0f) }
        waitFor("playing") { player.isPlaying }
        onMain { player.setVolume(0.01f) }
        assertTrue(onMain { player.isPlaying })
    }

    @Test
    fun loopsPastTheEndOfTheFile() {
        onMain { player.start(HEARTBEAT, 0f) }
        waitFor("playing") { player.isPlaying }
        onMain { player.seekToEnd(300) }
        // Past the end it wraps to the beginning instead of stopping.
        waitFor("wrapped", timeoutMs = 3_000) { player.positionMs in 1..(player.durationMs / 2) }
        assertTrue(onMain { player.isPlaying })
    }

    @Test
    fun stopsAndRestartsFromTheBeginning() {
        onMain { player.start(HEARTBEAT, 0f) }
        waitFor("playing") { player.isPlaying }
        SystemClock.sleep(700)
        onMain { player.stop() }
        assertFalse(onMain { player.isPlaying })
        onMain { player.start(HEARTBEAT, 0f) }
        waitFor("playing again") { player.isPlaying }
        assertTrue("restarted near the beginning", onMain { player.positionMs } < 600)
    }

    @Test
    fun releaseIsIdempotentAndStartAfterReleaseDoesNothing() {
        onMain {
            player.start(HEARTBEAT, 0f)
            player.release()
            player.release()
            player.start(HEARTBEAT, 0f)
        }
        SystemClock.sleep(300)
        assertFalse(onMain { player.isPlaying })
    }

    @Test
    fun aMissingFileIsSilent() {
        onMain { player.start("missing.mp3", 0f) }
        SystemClock.sleep(300)
        assertFalse(onMain { player.isPlaying })
    }

    private companion object {
        const val HEARTBEAT = "heartbeat.mp3"
    }
}
