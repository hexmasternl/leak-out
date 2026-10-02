package nl.hexmaster.leakout.settings

/**
 * The one-time tips of the game (spec `game-play-onboarding`), in the order they are shown when
 * several are due at once. The [key] is what is stored, so it is part of the file format and must
 * never change; the enum names can.
 */
enum class TipId(val key: String) {
    Intro("intro"),
    Mandatory("mandatory"),
    Cross("cross"),
    Reservoir("reservoir"),
    ;

    companion object {
        /** The tips stored under [keys]; a key no tip has (an old or a future tip) is ignored. */
        fun fromKeys(keys: Set<String>): Set<TipId> = entries.filterTo(mutableSetOf()) { it.key in keys }
    }
}
