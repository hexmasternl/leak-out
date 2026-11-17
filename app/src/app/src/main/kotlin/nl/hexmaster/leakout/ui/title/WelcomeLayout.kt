package nl.hexmaster.leakout.ui.title

import androidx.compose.runtime.Immutable

/**
 * Sizes for the welcome screen, all in dp, derived from the space left inside the safe-drawing
 * insets. Kept free of Compose types so the fitting rules are unit tested on the JVM.
 *
 * In landscape the buttons form one row under the centred title. In [portrait] they form one
 * column, centred horizontally at the bottom of the screen, with the title at the top.
 */
@Immutable
data class WelcomeLayout(
    val titleSizeDp: Float,
    val gapDp: Float,
    val buttonFaceHeightDp: Float,
    val buttonWidthDp: Float,
    val buttonSpacingDp: Float,
    val buttonCount: Int = BUTTON_COUNT,
    val portrait: Boolean = false,
) {
    /** Height the title line box takes (line height 1.1). */
    val titleHeightDp: Float get() = titleSizeDp * TITLE_LINE_HEIGHT

    /** Height of the buttons: one button in a row, every button and the spacing between them in a column. */
    val buttonsHeightDp: Float
        get() = if (portrait) {
            buttonCount * (buttonFaceHeightDp + BUTTON_SHADOW_DP) + (buttonCount - 1) * buttonSpacingDp
        } else {
            buttonFaceHeightDp + BUTTON_SHADOW_DP
        }

    /** Height of all content: title, gap and the buttons. */
    val contentHeightDp: Float get() = titleHeightDp + gapDp + buttonsHeightDp

    /** Width of the buttons: the whole row in landscape, a single button in a portrait column. */
    val buttonRowWidthDp: Float
        get() = if (portrait) buttonWidthDp else buttonCount * buttonWidthDp + (buttonCount - 1) * buttonSpacingDp

    companion object {
        const val TITLE_LINE_HEIGHT = 1.1f

        /**
         * "LEAK OUT" in Wendy One with the wordmark's letter spacing measures about 4.8 em wide;
         * 5.2 leaves margin for the glow.
         */
        const val TITLE_WIDTH_EM = 5.2f
        const val BUTTON_SHADOW_DP = 6f
        // Play, Settings and Exit; the Daily Pipe button makes four.
        const val BUTTON_COUNT = 3
        const val MIN_TOUCH_TARGET_DP = 48f

        private const val MAX_TITLE_DP = 120f
        private const val MIN_TITLE_DP = 28f
        private const val TITLE_HEIGHT_SHARE = 0.3f
        private const val PORTRAIT_TITLE_HEIGHT_SHARE = 0.2f
        private const val MAX_BUTTON_FACE_DP = 56f
        private const val MIN_BUTTON_FACE_DP = MIN_TOUCH_TARGET_DP - BUTTON_SHADOW_DP

        /** Fraction of the available size the content may use; the rest is breathing room. */
        private const val USABLE_FRACTION = 0.9f

        /**
         * Computes the layout of [buttonCount] buttons for an available area of [widthDp] x [heightDp].
         * An area taller than it is wide gets the portrait layout. The title grows with the screen
         * but is capped by both the width it must fit in and the height left after the buttons, so
         * short screens shrink the title first and never the touch targets.
         */
        fun compute(widthDp: Float, heightDp: Float, buttonCount: Int = BUTTON_COUNT): WelcomeLayout =
            if (heightDp > widthDp) computePortrait(widthDp, heightDp, buttonCount) else computeLandscape(widthDp, heightDp, buttonCount)

        private fun computeLandscape(widthDp: Float, heightDp: Float, buttonCount: Int): WelcomeLayout {
            val usableWidth = widthDp * USABLE_FRACTION
            val usableHeight = heightDp * USABLE_FRACTION

            val buttonFace = (heightDp * 0.15f).coerceIn(MIN_BUTTON_FACE_DP, MAX_BUTTON_FACE_DP)
            val gap = (heightDp * 0.08f).coerceIn(8f, 32f)
            val buttonSpacing = (widthDp * 0.025f).coerceIn(12f, 24f)
            val buttonWidth = ((usableWidth - (buttonCount - 1) * buttonSpacing) / buttonCount).coerceIn(96f, 200f)

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
                buttonCount = buttonCount,
            )
        }

        private fun computePortrait(widthDp: Float, heightDp: Float, buttonCount: Int): WelcomeLayout {
            val usableWidth = widthDp * USABLE_FRACTION
            val usableHeight = heightDp * USABLE_FRACTION

            val buttonFace = (heightDp * 0.07f).coerceIn(MIN_BUTTON_FACE_DP, MAX_BUTTON_FACE_DP)
            val buttonSpacing = (heightDp * 0.02f).coerceIn(12f, 20f)
            val gap = (heightDp * 0.06f).coerceIn(16f, 48f)
            // One column of equal buttons: wide enough for every label, never wider than the screen.
            val buttonWidth = minOf((usableWidth * 0.75f).coerceIn(160f, 320f), usableWidth)
                .coerceAtLeast(MIN_TOUCH_TARGET_DP)
            val stackHeight = buttonCount * (buttonFace + BUTTON_SHADOW_DP) + (buttonCount - 1) * buttonSpacing

            val titleByWidth = usableWidth / TITLE_WIDTH_EM
            val titleByHeight = (usableHeight - gap - stackHeight) / TITLE_LINE_HEIGHT
            val titleByShare = heightDp * PORTRAIT_TITLE_HEIGHT_SHARE
            val title = minOf(titleByWidth, titleByHeight, titleByShare, MAX_TITLE_DP)
                .coerceAtLeast(MIN_TITLE_DP)

            return WelcomeLayout(
                titleSizeDp = title,
                gapDp = gap,
                buttonFaceHeightDp = buttonFace,
                buttonWidthDp = buttonWidth,
                buttonSpacingDp = buttonSpacing,
                buttonCount = buttonCount,
                portrait = true,
            )
        }
    }
}
