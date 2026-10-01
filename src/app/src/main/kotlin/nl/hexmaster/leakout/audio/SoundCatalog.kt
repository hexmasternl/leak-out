package nl.hexmaster.leakout.audio

/**
 * Which files in `assets/sfx/` play for each [GameSound]. A file belongs to the group named by its
 * stem without a trailing `_NNN`, so adding a variant is only adding a file. A sound without files
 * has no variants and stays silent.
 */
class SoundCatalog(private val variants: Map<GameSound, List<String>>) {
    /** The files of [sound], in name order; empty when none are bundled. */
    fun variantsOf(sound: GameSound): List<String> = variants[sound].orEmpty()

    /** Every file in the catalogue, for loading them up front. */
    val files: List<String> get() = variants.values.flatten()

    companion object {
        /** The folder under `assets/` the effects ship in. */
        const val ASSET_DIR = "sfx"

        private val VARIANT_SUFFIX = Regex("_\\d{3}$")

        /** The catalogue of [fileNames] (bare names, no folder). Non-MP3 files and unknown groups are ignored. */
        fun from(fileNames: Iterable<String>): SoundCatalog {
            val grouped = fileNames
                .filter { it.endsWith(".mp3") }
                .sorted()
                .mapNotNull { file -> GameSound.ofGroup(groupOf(file))?.let { it to file } }
                .groupBy({ it.first }, { it.second })
            return SoundCatalog(grouped)
        }

        /** The group of [fileName]: the stem without a trailing `_NNN` of exactly three digits. */
        fun groupOf(fileName: String): String = fileName.removeSuffix(".mp3").replace(VARIANT_SUFFIX, "")
    }
}
