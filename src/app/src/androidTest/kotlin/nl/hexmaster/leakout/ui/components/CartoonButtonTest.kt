package nl.hexmaster.leakout.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class CartoonButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private val sources = CartoonButtonVariant.entries.associateWith { MutableInteractionSource() }

    @Before
    fun showAllVariants() {
        compose.setContent {
            LeakOutTheme {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CartoonButtonVariant.entries.forEach { variant ->
                        CartoonButton(
                            text = variant.name,
                            onClick = {},
                            variant = variant,
                            interactionSource = sources.getValue(variant),
                            modifier = Modifier.width(200.dp).testTag(variant.name),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun restingButtonShowsItsShadowBandAndFace() {
        CartoonButtonVariant.entries.forEach { variant ->
            val image = compose.onNodeWithTag(variant.name).captureToImage()
            // 5dp above the bottom lies inside the 6dp shadow band when resting.
            assertColourNear(variant.name, variant.shadow, image, fromBottomDp = 5f)
            assertColourNear(variant.name, variant.face, image, fromBottomDp = 16f)
        }
    }

    @Test
    fun pressedFaceSlidesDownOverTheShadow() {
        compose.mainClock.autoAdvance = false
        CartoonButtonVariant.entries.forEach { variant ->
            sources.getValue(variant).tryEmit(PressInteraction.Press(Offset(10f, 10f)))
        }
        compose.mainClock.advanceTimeBy(500)
        CartoonButtonVariant.entries.forEach { variant ->
            val image = compose.onNodeWithTag(variant.name).captureToImage()
            // The face moved down 4dp, so its ink border now covers the band that was shadow.
            val pixel = pixelAt(image, fromBottomDp = 5f)
            assertFalse("${variant.name}: shadow still visible at 5dp, got $pixel", near(pixel, variant.shadow))
            // 2dp of shadow is still visible at the very bottom.
            assertColourNear(variant.name, variant.shadow, image, fromBottomDp = 1f)
        }
    }

    @Test
    fun touchTargetIsAtLeast48dp() {
        CartoonButtonVariant.entries.forEach { variant ->
            compose.onNodeWithTag(variant.name)
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    private fun pixelAt(image: ImageBitmap, fromBottomDp: Float): Color {
        val density = compose.density.density
        val y = (image.height - fromBottomDp * density).toInt().coerceIn(0, image.height - 1)
        // A quarter in from the left: away from the rounded corners and from the centred label.
        return image.toPixelMap()[image.width / 4, y]
    }

    private fun assertColourNear(label: String, expected: Color, image: ImageBitmap, fromBottomDp: Float) {
        val actual = pixelAt(image, fromBottomDp)
        assertTrue("$label: expected $expected at ${fromBottomDp}dp from bottom, got $actual", near(actual, expected))
    }

    private fun near(a: Color, b: Color): Boolean =
        abs(a.red - b.red) < 0.06f && abs(a.green - b.green) < 0.06f && abs(a.blue - b.blue) < 0.06f
}
