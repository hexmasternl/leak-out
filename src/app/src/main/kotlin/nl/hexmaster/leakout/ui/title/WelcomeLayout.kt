package nl.hexmaster.leakout.ui.title

import androidx.compose.runtime.Immutable

/**
 * Sizes for the welcome screen, all in dp, derived from the space left inside the safe-drawing
 * insets. Kept free of Compose types so the fitting rules are unit tested on the JVM.
 */
@Immutable
data class WelcomeLayout(
    val titleSizeDp: Float,
    val gapDp: Float,
    val buttonFaceHeightDp: Float,
    val buttonWidthDp: Float,
    val buttonSpacingDp: Float,
) {
    /** Height the title line box takes (line height 1.1). */
    val titleHeightDp: Float get() = titleSizeDp * TITLE_LINE_HEIGHT

    /** Height of the whole centred column: title, gap, and a button including its shadow. */
    val contentHeightDp: Float get() = titleHeightDp + gapDp + buttonFaceHeightDp + BUTTON_SHADOW_DP

    /** Width of the three-button row. */
    val buttonRowWidthDp: Float get() = BUTTON_COUNT * buttonWidthDp + (BUTTON_COUNT - 1) * buttonSpacingDp

    companion object {
        const val TITLE_LINE_HEIGHT = 1.1f

        /**
         * "LEAK OUT" in Wendy One with the wordmark's letter spacing measures about 4.8 em wide;
         * 5.2 leaves margin for the glow.
         */
        const val TITLE_WIDTH_EM = 5.2f
        const val BUTTON_SHADOW_DP = 6f
        const val BUTTON_COUNT = 3
        const val MIN_TOUCH_TARGET_DP = 48f

        private const val MAX_TITLE_DP = 120f
        private const val MIN_TITLE_DP = 28f
        private const val TITLE_HEIGHT_SHARE = 0.3f
        private const val MAX_BUTTON_FACE_DP = 56f
        private const val MIN_BUTTON_FACE_DP = MIN_TOUCH_TARGET_DP - BUTTON_SHADOW_DP

        /** Fraction of the available size the content may use; the rest is breathing room. */
        private const val USABLE_FRACTION = 0.9f

        /**
         * Computes the layout for an available area of [widthDp] x [heightDp]. The title grows
         * with the screen but is capped by both the width it must fit in and the height left
         * after the buttons, so short landscape phones shrink the title first and never the
         * touch targets.
         */
        fun compute(widthDp: Float, heightDp: Float): WelcomeLayout {
            val usableWidth = widthDp * USABLE_FRACTION
            val usableHeight = heightDp * USABLE_FRACTION

            val buttonFace = (heightDp * 0.15f).coerceIn(MIN_BUTTON_FACE_DP, MAX_BUTTON_FACE_DP)
            val gap = (heightDp * 0.08f).coerceIn(8f, 32f)
            val buttonSpacing = (widthDp * 0.025f).coerceIn(12f, 24f)
            val buttonWidth = ((usableWidth - 2 * buttonSpacing) / BUTTON_COUNT).coerceIn(96f, 200f)

            val titleByWidth = usableWidth / TITLE_WIDTH_EM
            val titleByHeight = (usableHeight - gap - buttonFace - BUTTON_SHADOW_DP) / TITLE_LINE_HEIGHT
            // Without this share cap the title would swallow half of a phone's landscape height.
            val titleByShare = heightDp * TITLE_HEIGHT_SHARE
            val title = minOf(titleByWidth, titleByHeight, titleByShare, MAX_TITLE_DP)
                .coerceAtLeast(MIN_TITLE_DP)

            return WelcomeLayout(
                titleSizeDp = title,
                gapDp = gap,
                buttonFaceHeightDp = buttonFace,
                buttonWidthDp = buttonWidth,
                buttonSpacingDp = buttonSpacing,
            )
        }
    }
}
