package nl.hexmaster.leakout

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.audio.BundledMusic
import nl.hexmaster.leakout.audio.DataStoreMusicRotationStore
import nl.hexmaster.leakout.audio.MusicRotation
import nl.hexmaster.leakout.settings.DataStoreSettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The music rotation through the real activity, DataStore and MediaPlayer: each launch of a new
 * activity (a new MusicViewModel, as on a cold start) starts one track further, and a stop/start or
 * a recreation resumes without moving the stored id.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityMusicTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settings = DataStoreSettingsRepository.create(context)
    private val store = DataStoreMusicRotationStore.create(context)
    private val rotation = MusicRotation(checkNotNull(BundledMusic.load(context.assets)))
    private var musicWasEnabled = true
    private var musicVolume = 0f

    @Before
    fun enableMusicQuietly() = runBlocking {
        val stored = settings.settings.first()
        musicWasEnabled = stored.musicEnabled
        musicVolume = stored.musicVolume
        settings.setMusicEnabled(true)
        // Silent on the device; the player still starts every track.
        settings.setMusicVolume(0f)
    }

    @After
    fun restoreSettings() = runBlocking {
        settings.setMusicEnabled(musicWasEnabled)
        settings.setMusicVolume(musicVolume)
    }

    private fun storedId(): String? = runBlocking { store.lastStartedTrackId() }

    private fun waitForStoredId(expected: String) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (storedId() != expected) {
            assertTrue("timed out waiting for $expected, stored ${storedId()}", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(50)
        }
    }

    /** Launches the activity and waits until the track after the stored one has started; returns its id. */
    private fun launchAndAwaitNextTrack(): Pair<ActivityScenario<MainActivity>, String> {
        val expected = rotation.first(storedId()).id
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForStoredId(expected)
        return scenario to expected
    }

    @Test
    fun eachLaunchAdvancesTheStoredTrackByOne() {
        val before = storedId()
        val (first, firstId) = launchAndAwaitNextTrack()
        first.close()
        val (second, secondId) = launchAndAwaitNextTrack()
        second.close()

        assertEquals(rotation.first(before).id, firstId)
        assertEquals(rotation.after(rotation.first(before)).id, secondId)
        assertEquals(secondId, storedId())
    }

    @Test
    fun stopStartAndRecreationKeepTheStoredTrack() {
        val (scenario, id) = launchAndAwaitNextTrack()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            // Long enough for a wrongly started new track to have been stored.
            SystemClock.sleep(2_000)
            assertEquals(id, storedId())
        } finally {
            scenario.close()
        }
    }
}
