package nl.hexmaster.leakout.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A [MusicEngine] that plays in memory. A track starts as soon as it is opened, unless [failing] names it. */
private class FakeEngine : MusicEngine {
    override var listener: MusicEngine.Listener? = null
    val opened = mutableListOf<String>()
    var track: Track? = null
    var playing = false
    var level = -1f
    var resumes = 0
    var released = false
    val failing = mutableSetOf<String>()

    override fun play(track: Track) {
        opened += track.id
        this.track = track
        if (track.id in failing) {
            this.track = null
            playing = false
            listener?.onError(track)
            return
        }
        playing = true
        listener?.onStarted(track)
    }

    override fun pause() {
        playing = false
    }

    override fun resume() {
        if (track == null) return
        resumes++
        playing = true
    }

    override fun setVolume(volume: Float) {
        level = volume
    }

    override fun release() {
        released = true
        track = null
        playing = false
    }

    /** The open track plays to its end. */
    fun complete() = listener?.onCompleted(track!!)

    val playingId: String? get() = track?.id?.takeIf { playing }
}

/** [AudioFocus] that grants every request unless [grant] is off, and lets the test send focus changes. */
private class FakeFocus : AudioFocus {
    var grant = true
    var requests = 0
    var abandons = 0
    var held = false
    private var onChange: ((FocusChange) -> Unit)? = null

    override fun request(onChange: (FocusChange) -> Unit): Boolean {
        requests++
        this.onChange = onChange
        held = grant
        return grant
    }

    override fun abandon() {
        abandons++
        held = false
    }

    fun send(change: FocusChange) = onChange!!(change)
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundMusicTest {
    private val playlist = Playlist((1..10).map { Track("bg_music_%04d.mp3".format(it), "Title $it", "Artist $it") })
    private fun id(n: Int) = "bg_music_%04d".format(n)

    private val store = InMemoryMusicRotationStore()
    private var engine = FakeEngine()
    private var focus = FakeFocus()

    /** A cold start: a new engine, focus and controller on the same [store], in the foreground. */
    private fun TestScope.launch(
        settings: Settings? = Settings.Default,
        list: Playlist? = playlist,
        configure: FakeEngine.() -> Unit = {},
    ): BackgroundMusic {
        engine = FakeEngine().apply(configure)
        focus = FakeFocus()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        return BackgroundMusic(list, store, engine, focus, scope).apply {
            settings?.let(::apply)
            onForeground()
        }
    }

    @Test
    fun `the first launch plays the first track and stores it`() = runTest {
        launch()
        assertEquals(id(1), engine.playingId)
        assertEquals(id(1), store.lastStartedTrackId())
        assertTrue(focus.held)
    }

    @Test
    fun `the next launch continues the rotation`() = runTest {
        store.setLastStartedTrackId(id(4))
        launch()
        assertEquals(id(5), engine.playingId)
        assertEquals(id(5), store.lastStartedTrackId())
    }

    @Test
    fun `the rotation wraps across launches`() = runTest {
        store.setLastStartedTrackId(id(10))
        launch()
        assertEquals(id(1), engine.playingId)
    }

    @Test
    fun `an unknown stored id starts at the first track`() = runTest {
        store.setLastStartedTrackId("removed_track")
        launch()
        assertEquals(id(1), engine.playingId)
    }

    @Test
    fun `every launch starts one track further`() = runTest {
        val started = (1..12).map {
            launch().release()
            engine.opened.single()
        }
        assertEquals((1..10).map(::id) + id(1) + id(2), started)
    }

    @Test
    fun `later tracks in a session are recorded`() = runTest {
        store.setLastStartedTrackId(id(1))
        launch()
        assertEquals(id(2), engine.playingId)
        engine.complete()
        assertEquals(id(3), engine.playingId)
        assertEquals(id(3), store.lastStartedTrackId())

        launch()
        assertEquals(id(4), engine.playingId)
    }

    @Test
    fun `a finished track is followed by the next and the last wraps to the first`() = runTest {
        store.setLastStartedTrackId(id(2))
        launch()
        engine.complete()
        assertEquals(id(4), engine.playingId)

        store.setLastStartedTrackId(id(9))
        launch()
        assertEquals(id(10), engine.playingId)
        engine.complete()
        assertEquals(id(1), engine.playingId)
    }

    @Test
    fun `going to the background pauses and coming back resumes without advancing`() = runTest {
        store.setLastStartedTrackId(id(5))
        val music = launch()
        assertEquals(id(6), engine.playingId)

        music.onBackground()
        assertNull(engine.playingId)
        assertEquals(id(6), engine.track!!.id)
        assertFalse(focus.held)

        music.onForeground()
        assertEquals(id(6), engine.playingId)
        assertEquals(1, engine.resumes)
        assertEquals(listOf(id(6)), engine.opened)
        assertEquals(listOf(id(5), id(6)), store.writes)
        assertTrue(focus.held)
    }

    @Test
    fun `nothing plays before the settings are read`() = runTest {
        launch(settings = null)
        assertNull(engine.track)
        assertEquals(0, focus.requests)
    }

    @Test
    fun `music disabled at launch plays nothing and leaves the rotation alone`() = runTest {
        store.setLastStartedTrackId(id(4))
        launch(Settings(musicEnabled = false))
        assertNull(engine.track)
        assertEquals(0, focus.requests)
        assertEquals(listOf(id(4)), store.writes)

        // The next launch with music on continues from track 4.
        launch()
        assertEquals(id(5), engine.playingId)
    }

    @Test
    fun `turning music off pauses and turning it on resumes the same track`() = runTest {
        val music = launch()
        music.apply(Settings(musicEnabled = false))
        assertNull(engine.playingId)
        assertFalse(focus.held)

        music.apply(Settings(musicEnabled = true))
        assertEquals(id(1), engine.playingId)
        assertEquals(1, engine.resumes)
        assertEquals(listOf(id(1)), engine.opened)
        assertEquals(listOf(id(1)), store.writes)
    }

    @Test
    fun `enabling music in a silent session starts the rotation's track`() = runTest {
        store.setLastStartedTrackId(id(2))
        val music = launch(Settings(musicEnabled = false))
        music.apply(Settings(musicEnabled = true))
        assertEquals(id(3), engine.playingId)
        assertEquals(id(3), store.lastStartedTrackId())
    }

    @Test
    fun `the volume follows the settings and the slider preview`() = runTest {
        val music = launch()
        assertEquals(Settings.DEFAULT_MUSIC_VOLUME, engine.level)
        music.apply(Settings(musicVolume = 0.3f))
        assertEquals(0.3f, engine.level)
        assertEquals(id(1), engine.playingId)
        music.previewVolume(0.1f)
        assertEquals(0.1f, engine.level)
        music.previewVolume(4f)
        assertEquals(1f, engine.level)
    }

    @Test
    fun `a permanent focus loss pauses until the next foreground`() = runTest {
        val music = launch()
        focus.send(FocusChange.Loss)
        assertNull(engine.playingId)
        assertEquals(1, focus.abandons)

        // A settings emission is not a reason to take the focus back.
        music.apply(Settings(musicVolume = 0.5f))
        assertNull(engine.playingId)
        assertEquals(1, focus.requests)

        music.onBackground()
        music.onForeground()
        assertEquals(id(1), engine.playingId)
        assertEquals(listOf(id(1)), engine.opened)
    }

    @Test
    fun `a transient focus loss pauses and the gain resumes the same track`() = runTest {
        launch()
        focus.send(FocusChange.TransientLoss)
        assertNull(engine.playingId)
        assertTrue("kept through a transient loss", focus.held)

        focus.send(FocusChange.Gain)
        assertEquals(id(1), engine.playingId)
        assertEquals(listOf(id(1)), engine.opened)
        assertEquals(1, focus.requests)
    }

    @Test
    fun `ducking plays at a fifth of the set volume until focus returns`() = runTest {
        launch(Settings(musicVolume = 0.5f))
        focus.send(FocusChange.CanDuck)
        assertEquals(0.1f, engine.level, 0.0001f)
        assertEquals(id(1), engine.playingId)

        focus.send(FocusChange.Gain)
        assertEquals(0.5f, engine.level)
    }

    @Test
    fun `denied focus keeps the music silent until the next foreground`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        focus.grant = false
        val music = BackgroundMusic(playlist, store, engine, focus, scope)
        music.apply(Settings.Default)
        music.onForeground()
        assertNull(engine.track)
        music.apply(Settings(musicVolume = 0.2f))
        assertEquals(1, focus.requests)

        focus.grant = true
        music.onBackground()
        music.onForeground()
        assertEquals(id(1), engine.playingId)
    }

    @Test
    fun `a broken track is skipped without being stored`() = runTest {
        store.setLastStartedTrackId(id(4))
        launch { failing += id(5) }
        assertEquals(id(6), engine.playingId)
        assertEquals(listOf(id(5), id(6)), engine.opened)
        assertEquals(listOf(id(4), id(6)), store.writes)
    }

    @Test
    fun `when every track fails the music stays silent`() = runTest {
        launch { failing += (1..10).map(::id) }
        assertNull(engine.playingId)
        assertEquals((1..10).map(::id), engine.opened)
        assertTrue(store.writes.isEmpty())
        assertFalse(focus.held)
    }

    @Test
    fun `without a playlist nothing plays`() = runTest {
        launch(list = null)
        assertNull(engine.track)
        assertEquals(0, focus.requests)
    }

    @Test
    fun `release frees the player and the focus`() = runTest {
        val music = launch()
        music.release()
        assertTrue(engine.released)
        assertFalse(focus.held)
        assertNull(engine.listener)
    }
}
