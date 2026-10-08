package nl.hexmaster.leakout.audio

/**
 * Which track plays next. The rotation is keyed on the id of the last track that started, so
 * every launch begins one track further and reordering the playlist never points at a wrong song.
 */
class MusicRotation(private val playlist: Playlist) {
    /** The track to open a session with: the one after [lastStartedId], or the first when that id is null or unknown. */
    fun first(lastStartedId: String?): Track {
        val index = lastStartedId?.let(playlist::indexOf) ?: -1
        return if (index < 0) playlist.tracks.first() else at(index + 1)
    }

    /** The track after [track], wrapping from the last to the first. */
    fun after(track: Track): Track = at(playlist.indexOf(track.id) + 1)

    private fun at(index: Int): Track = playlist.tracks[index.mod(playlist.tracks.size)]
}
