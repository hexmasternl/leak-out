package nl.hexmaster.leakout.audio

/**
 * Plays nothing and remembers every [play], for tests and previews, like the in-memory
 * repositories. [played] lists the files in order; [plays] adds the volume each was played at.
 */
class RecordingSoundEffectPlayer : SoundEffectPlayer {
    data class Play(val file: String, val volume: Float)

    val plays = mutableListOf<Play>()

    val played: List<String> get() = plays.map { it.file }

    var released = false
        private set

    override fun play(file: String, volume: Float) {
        plays += Play(file, volume)
    }

    override fun release() {
        released = true
    }
}
