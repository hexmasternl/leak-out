package nl.hexmaster.leakout.audio

/**
 * The moments in a game that play a sound effect. [group] is the file name stem the effects in
 * `assets/sfx/` share, without their `_NNN` variant number. A [looping] sound plays through
 * [SoundEffects.heartbeat]'s looping player instead of as a one-shot effect.
 */
enum class GameSound(val group: String, val looping: Boolean = false) {
    /** A section placed on an empty tile. */
    SectionPlaced("section_placement"),

    /** A section placed on a tile that held one, which destroys the old section. */
    SectionDestroyed("section_destroy"),

    /** A drop or tap on a tile that refuses the section. */
    PlacementFailed("section_placement_failed"),

    /** The level complete dialog appears. */
    LevelComplete("level_complete"),

    /** The level lost dialog appears. */
    LevelFailed("level_failed"),

    /** Loops while the liquid closes in on a gap (spec `game-play-gap-warning`). */
    Heartbeat("heartbeat", looping = true),
    ;

    companion object {
        fun ofGroup(group: String): GameSound? = entries.firstOrNull { it.group == group }
    }
}
