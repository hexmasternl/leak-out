package nl.hexmaster.leakout.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicRotationTest {
    private val tracks = (1..10).map { Track("bg_music_%04d.mp3".format(it), "Title $it", "Artist $it") }
    private val rotation = MusicRotation(Playlist(tracks))

    @Test
    fun `nothing stored starts with the first track`() {
        assertEquals(tracks[0], rotation.first(null))
    }

    @Test
    fun `starts with the track after the last one started`() {
        assertEquals(tracks[4], rotation.first("bg_music_0004"))
    }

    @Test
    fun `wraps from the last track to the first across launches`() {
        assertEquals(tracks[0], rotation.first("bg_music_0010"))
    }

    @Test
    fun `an unknown stored id starts with the first track`() {
        assertEquals(tracks[0], rotation.first("removed_track"))
    }

    @Test
    fun `after advances and wraps`() {
        assertEquals(tracks[3], rotation.after(tracks[2]))
        assertEquals(tracks[0], rotation.after(tracks[9]))
    }

    @Test
    fun `a single track follows itself`() {
        val solo = MusicRotation(Playlist(tracks.take(1)))
        assertEquals(tracks[0], solo.first("bg_music_0001"))
        assertEquals(tracks[0], solo.after(tracks[0]))
    }
}
