package nl.hexmaster.leakout.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioCreditsParserTest {
    @Test
    fun `parses tracks in order with the file stem as id`() {
        val playlist = AudioCreditsParser.parse(
            """
            [
              { "title": "Ebb and Flow", "artist": "Fabian Measures", "file": "bg_music_0001.mp3" },
              { "title": "Butterfly", "artist": "Kate Kody", "file": "bg_music_0002.mp3" }
            ]
            """,
        )!!
        assertEquals(listOf("bg_music_0001", "bg_music_0002"), playlist.tracks.map { it.id })
        assertEquals(Track("bg_music_0001.mp3", "Ebb and Flow", "Fabian Measures"), playlist.tracks[0])
    }

    @Test
    fun `unknown fields are ignored`() {
        val playlist = AudioCreditsParser.parse("""[{ "title": "A", "artist": "B", "file": "a.mp3", "license": "CC BY 4.0" }]""")
        assertEquals(1, playlist?.tracks?.size)
    }

    @Test
    fun `an empty list is no playlist`() {
        assertNull(AudioCreditsParser.parse("[]"))
    }

    @Test
    fun `invalid json is no playlist`() {
        assertNull(AudioCreditsParser.parse("[{ \"title\": "))
        assertNull(AudioCreditsParser.parse(""))
        assertNull(AudioCreditsParser.parse("""{ "title": "A", "artist": "B", "file": "a.mp3" }"""))
    }

    @Test
    fun `a missing or blank field is no playlist`() {
        assertNull(AudioCreditsParser.parse("""[{ "title": "A", "file": "a.mp3" }]"""))
        assertNull(AudioCreditsParser.parse("""[{ "title": "A", "artist": " ", "file": "a.mp3" }]"""))
        assertNull(AudioCreditsParser.parse("""[{ "title": "A", "artist": 3, "file": "a.mp3" }]"""))
    }

    @Test
    fun `the same file twice is no playlist`() {
        assertNull(
            AudioCreditsParser.parse(
                """[{ "title": "A", "artist": "B", "file": "a.mp3" }, { "title": "C", "artist": "D", "file": "a.mp3" }]""",
            ),
        )
    }
}
