package nl.hexmaster.leakout.audio

/** One background track: its audio file in `assets/music/` and who made it. [id] is the file name without the extension. */
data class Track(
    val file: String,
    val title: String,
    val artist: String,
) {
    val id: String get() = file.substringBeforeLast('.')
}

/** The background tracks in playback order. Never empty, and no two tracks share an id. */
class Playlist(tracks: List<Track>) {
    val tracks: List<Track> = tracks.toList()

    init {
        require(this.tracks.isNotEmpty()) { "A playlist needs at least one track" }
        require(this.tracks.map { it.id }.toSet().size == this.tracks.size) { "Track ids must be unique" }
    }

    fun indexOf(id: String): Int = tracks.indexOfFirst { it.id == id }
}
