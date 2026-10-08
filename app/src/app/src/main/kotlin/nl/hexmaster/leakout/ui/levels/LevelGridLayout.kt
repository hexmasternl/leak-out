package nl.hexmaster.leakout.ui.levels

import nl.hexmaster.leakout.core.library.LevelLibraryLayout

/** Tile size and gap of the 6x4 grid, in dp. */
data class LevelGridMetrics(val tileWidthDp: Float, val tileHeightDp: Float, val gapDp: Float) {
    val gridWidthDp: Float get() = tileWidthDp * LevelLibraryLayout.COLUMNS + gapDp * (LevelLibraryLayout.COLUMNS - 1)
    val gridHeightDp: Float get() = tileHeightDp * LevelLibraryLayout.ROWS + gapDp * (LevelLibraryLayout.ROWS - 1)
}

/**
 * Sizes the level grid so all 24 tiles fit without scrolling. Tiles fill their cell up to the
 * design system's 88 x 104 dp, so on a short landscape screen they become wider than tall.
 * The chrome around the grid (header, hint, page buttons) is fixed here so the screen and the
 * fit check agree.
 */
object LevelGridLayout {
    const val MAX_TILE_WIDTH_DP = 88f
    const val MAX_TILE_HEIGHT_DP = 104f
    const val GAP_DP = 8f

    const val SCREEN_PADDING_H_DP = 16f
    const val SCREEN_PADDING_V_DP = 8f
    const val HEADER_HEIGHT_DP = 36f
    const val HINT_HEIGHT_DP = 28f
    const val NAV_BUTTON_WIDTH_DP = 56f
    const val SECTION_SPACING_DP = 8f

    /** Metrics for a grid area of [widthDp] x [heightDp]. */
    fun compute(widthDp: Float, heightDp: Float): LevelGridMetrics {
        val columns = LevelLibraryLayout.COLUMNS
        val rows = LevelLibraryLayout.ROWS
        val cellWidth = (widthDp - GAP_DP * (columns - 1)) / columns
        val cellHeight = (heightDp - GAP_DP * (rows - 1)) / rows
        return LevelGridMetrics(
            tileWidthDp = cellWidth.coerceIn(0f, MAX_TILE_WIDTH_DP),
            tileHeightDp = cellHeight.coerceIn(0f, MAX_TILE_HEIGHT_DP),
            gapDp = GAP_DP,
        )
    }

    /** The grid area left on a screen (inside the safe area) of [widthDp] x [heightDp]. */
    fun gridArea(widthDp: Float, heightDp: Float): Pair<Float, Float> {
        val width = widthDp - 2 * SCREEN_PADDING_H_DP - 2 * (NAV_BUTTON_WIDTH_DP + SECTION_SPACING_DP)
        val height = heightDp - 2 * SCREEN_PADDING_V_DP - HEADER_HEIGHT_DP - HINT_HEIGHT_DP - 2 * SECTION_SPACING_DP
        return width to height
    }

    /** Metrics for a whole screen of [widthDp] x [heightDp]. */
    fun forScreen(widthDp: Float, heightDp: Float): LevelGridMetrics {
        val (width, height) = gridArea(widthDp, heightDp)
        return compute(width, height)
    }
}
