package nl.hexmaster.leakout.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.settings.Settings

/**
 * Decides what the background music does: which track, when it starts, pauses, resumes and skips,
 * and how loud it is. It plays only while the app is in the foreground, music is on, the settings
 * have been read and the app holds audio focus. [MusicEngine] only carries the decisions out.
 *
 * The rotation: the first track of a session is the one after the stored last-started id; each
 * track that starts fresh is stored, a resume is not. A track that fails is skipped without being
 * stored, and once every track has failed in a session the music stays silent.
 *
 * Call it from the main thread; [scope] runs the store reads and writes. A null [playlist] (no
 * readable credits file) means silence.
 */
class BackgroundMusic(
    private val playlist: Playlist?,
    private val store: MusicRotationStore,
    private val engine: MusicEngine,
    private val focus: AudioFocus,
    private val scope: CoroutineScope,
) {
    private val rotation = playlist?.let(::MusicRotation)

    private var settingsRead = false
    private var enabled = Settings.DEFAULT_MUSIC_ENABLED
    private var volume = Settings.DEFAULT_MUSIC_VOLUME
    private var foreground = false

    private var hasFocus = false
    // Focus denied or lost for good: no new request until the next foreground or until music is turned on again.
    private var focusBlocked = false
    private var transientLoss = false
    private var ducking = false

    /** The track open in the engine, or null when none is. */
    private var current: Track? = null
    // Whether the engine was last told to play rather than pause.
    private var playing = false
    // The track to open next, after a completion or a failure.
    private var next: Track? = null
    private var rotationRead = false
    private var readingRotation = false
    private var lastStartedId: String? = null
    private val failed = mutableSetOf<String>()
    private var gaveUp = false

    init {
        engine.listener = object : MusicEngine.Listener {
            override fun onStarted(track: Track) {
                if (track != current) return
                lastStartedId = track.id
                scope.launch { store.setLastStartedTrackId(track.id) }
            }

            override fun onCompleted(track: Track) {
                if (track != current) return
                moveOn(rotation!!.after(track))
            }

            override fun onError(track: Track) {
                if (track != current) return
                failed += track.id
                if (failed.size >= playlist!!.tracks.size) {
                    gaveUp = true
                    current = null
                    playing = false
                    update()
                } else {
                    moveOn(rotation!!.after(track))
                }
            }
        }
        applyVolume()
    }

    /** Takes over the stored music settings; on and off and the volume count straight away. */
    fun apply(settings: Settings) {
        val turnedOn = settings.musicEnabled && (!enabled || !settingsRead)
        settingsRead = true
        enabled = settings.musicEnabled
        volume = Settings.clampVolume(settings.musicVolume)
        if (turnedOn) focusBlocked = false
        applyVolume()
        update()
    }

    /** Plays at [volume] while the music slider is dragged; the stored volume takes over when it arrives. */
    fun previewVolume(volume: Float) {
        this.volume = Settings.clampVolume(volume)
        applyVolume()
    }

    /** The app came to the foreground: ask for focus again and start or resume. */
    fun onForeground() {
        foreground = true
        focusBlocked = false
        update()
    }

    /** The app left the foreground: pause in place and give the focus back. */
    fun onBackground() {
        foreground = false
        update()
        releaseFocus()
    }

    /** Frees the player and the focus. */
    fun release() {
        engine.listener = null
        engine.release()
        current = null
        playing = false
        releaseFocus()
    }

    private fun onFocusChange(change: FocusChange) {
        when (change) {
            FocusChange.Gain -> {
                hasFocus = true
                transientLoss = false
                ducking = false
            }
            FocusChange.Loss -> {
                releaseFocus()
                focusBlocked = true
            }
            FocusChange.TransientLoss -> transientLoss = true
            FocusChange.CanDuck -> ducking = true
        }
        applyVolume()
        update()
    }

    private fun moveOn(track: Track) {
        current = null
        playing = false
        next = track
        update()
    }

    private fun wantsToPlay(): Boolean =
        rotation != null && !gaveUp && settingsRead && enabled && foreground && !focusBlocked && !transientLoss

    private fun update() {
        if (!wantsToPlay()) {
            if (playing) {
                engine.pause()
                playing = false
            }
            // Kept through a transient loss, or the system would not hand it back.
            if (!transientLoss) releaseFocus()
            return
        }
        if (!holdFocus()) return
        val open = current
        when {
            open != null -> if (!playing) {
                engine.resume()
                playing = true
            }
            next != null -> open(next!!)
            rotationRead -> open(rotation!!.first(lastStartedId))
            else -> readRotation()
        }
    }

    private fun open(track: Track) {
        next = null
        current = track
        playing = true
        engine.play(track)
    }

    private fun readRotation() {
        if (readingRotation) return
        readingRotation = true
        scope.launch {
            lastStartedId = store.lastStartedTrackId()
            rotationRead = true
            readingRotation = false
            update()
        }
    }

    private fun holdFocus(): Boolean {
        if (hasFocus) return true
        hasFocus = focus.request(::onFocusChange)
        if (!hasFocus) focusBlocked = true
        return hasFocus
    }

    private fun releaseFocus() {
        if (!hasFocus) return
        focus.abandon()
        hasFocus = false
        transientLoss = false
        if (ducking) {
            ducking = false
            applyVolume()
        }
    }

    private fun applyVolume() = engine.setVolume(if (ducking) volume * DUCK_FACTOR else volume)

    companion object {
        /** The share of the set volume that plays while another app may duck the music. */
        const val DUCK_FACTOR = 0.2f
    }
}
