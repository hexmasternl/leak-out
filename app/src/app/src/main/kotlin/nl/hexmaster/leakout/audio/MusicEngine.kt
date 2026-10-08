package nl.hexmaster.leakout.audio

/**
 * Plays one background [Track] at a time. The seam between [BackgroundMusic], which makes every
 * decision, and `MediaPlayer`. Calls and callbacks happen on the main thread.
 */
interface MusicEngine {
    /** Receives what happened to the open track. */
    interface Listener {
        /** [track] started playing for the first time since [play]; a [resume] does not count. */
        fun onStarted(track: Track)

        /** [track] played to its end. */
        fun onCompleted(track: Track)

        /** [track] could not be opened or played; the engine has closed it. */
        fun onError(track: Track)
    }

    var listener: Listener?

    /** Closes any open track, then opens and prepares [track] asynchronously and starts it once ready. */
    fun play(track: Track)

    /** Pauses the open track and keeps its position. A track still preparing waits for [resume]. */
    fun pause()

    /** Continues the open track from where it paused. */
    fun resume()

    /** The playback volume, 0.0-1.0, for the open track and every track after it. */
    fun setVolume(volume: Float)

    /** Closes the open track and frees the player. Safe to call more than once. */
    fun release()
}
