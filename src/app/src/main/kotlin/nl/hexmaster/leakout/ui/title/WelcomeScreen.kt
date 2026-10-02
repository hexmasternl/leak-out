package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.library.ContinueTarget
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.core.library.NextLevelResolver
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object WelcomeScreenTags {
    const val SCREEN = "welcome_screen"
    const val TITLE = "welcome_title"
    const val PLAY = "welcome_play"
    const val SETTINGS = "welcome_settings"
    const val EXIT = "welcome_exit"
    const val LEVEL_DESIGNER = "welcome_level_designer"
}

/**
 * Launch screen: full-bleed art, the centred logo and the Play / Settings / Exit row below it.
 * Play opens the [PlayMenuDialog]; a choice there reads [resolveNextLevel] once, closes the dialog
 * and then starts a level through [onStartLevel] or opens the library on a page through
 * [onOpenLevelLibrary]. The host decides what Settings and Exit do.
 *
 * When [onOpenLevelDesigner] is not null (the level designer feature flag is on), a small level
 * designer button sits in the top-right corner; without it the button is not composed at all.
 *
 * @param resolveNextLevel a fresh snapshot of the next level to be played. When it throws, the
 *   dialog stays open and nothing is navigated to.
 */
@Composable
fun WelcomeScreen(
    resolveNextLevel: suspend () -> NextLevel,
    onStartLevel: (levelId: Int) -> Unit,
    onOpenLevelLibrary: (page: Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLevelDesigner: (() -> Unit)? = null,
) {
    var showPlayMenu by rememberSaveable { mutableStateOf(false) }
    // One choice at a time: a second tap while the history is read is ignored.
    var resolving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun choose(action: (NextLevel) -> Unit) {
        if (resolving) return
        resolving = true
        scope.launch {
            val next = try {
                resolveNextLevel()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } finally {
                resolving = false
            }
            if (next != null) {
                showPlayMenu = false
                action(next)
            }
        }
    }

    if (showPlayMenu) {
        PlayMenuDialog(
            onContinue = {
                choose { next ->
                    when (val target = next.continueTarget) {
                        is ContinueTarget.StartLevel -> onStartLevel(target.levelId)
                        is ContinueTarget.OpenLibrary -> onOpenLevelLibrary(target.page)
                    }
                }
            },
            onSelectLevel = { choose { next -> onOpenLevelLibrary(next.page) } },
            onDismiss = { showPlayMenu = false },
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag(WelcomeScreenTags.SCREEN)) {
        WelcomeBackground()
        // Art runs under the bars and cutout; interactive content stays inside the safe area.
        // The larger inset is applied to both sides so a one-sided camera cutout does not push
        // the logo off the screen's centre line.
        val safe = WindowInsets.safeDrawing
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        val horizontalInset = with(density) {
            maxOf(safe.getLeft(density, direction), safe.getRight(density, direction)).toDp()
        }
        val verticalInset = with(density) { maxOf(safe.getTop(density), safe.getBottom(density)).toDp() }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalInset, vertical = verticalInset)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val layout = remember(maxWidth, maxHeight) {
                WelcomeLayout.compute(maxWidth.value, maxHeight.value)
            }
            // Fixed like the logo: the button row is sized to fit, so font scaling must not break it.
            val buttonTextSize = with(LocalDensity.current) { BUTTON_TEXT_DP.dp.toSp() }
            val buttonTextStyle = LeakOutTextStyles.ButtonText.copy(fontSize = buttonTextSize)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LeakOutTitle(
                    text = stringResource(R.string.welcome_title),
                    sizeDp = layout.titleSizeDp,
                    modifier = Modifier.testTag(WelcomeScreenTags.TITLE),
                )
                Spacer(Modifier.height(layout.gapDp.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(layout.buttonSpacingDp.dp)) {
                    val buttonModifier = Modifier.width(layout.buttonWidthDp.dp)
                    CartoonButton(
                        text = stringResource(R.string.welcome_play),
                        onClick = { showPlayMenu = true },
                        variant = CartoonButtonVariant.Secondary,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.PLAY),
                    )
                    CartoonButton(
                        text = stringResource(R.string.welcome_settings),
                        onClick = onSettings,
                        variant = CartoonButtonVariant.Primary,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.SETTINGS),
                    )
                    CartoonButton(
                        text = stringResource(R.string.welcome_exit),
                        onClick = onExit,
                        variant = CartoonButtonVariant.Danger,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.EXIT),
                    )
                }
            }
        }
        // A developer tool, out of the way of the Play / Settings / Exit layout.
        if (onOpenLevelDesigner != null) {
            LevelDesignerButton(
                onClick = onOpenLevelDesigner,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = horizontalInset, vertical = verticalInset)
                    .padding(8.dp),
            )
        }
    }
}

/** The 48 dp round glass button that opens the level designer (design system 5.1). */
@Composable
private fun LevelDesignerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.welcome_level_designer)
    Box(
        modifier = modifier
            .testTag(WelcomeScreenTags.LEVEL_DESIGNER)
            .size(48.dp)
            .background(LeakOutColors.SurfaceOverlay.copy(alpha = 0.8f), CircleShape)
            .border(1.dp, LeakOutColors.LineStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_designer),
            contentDescription = null,
            tint = LeakOutColors.TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private const val BUTTON_TEXT_DP = 18f

@Preview(name = "Welcome 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun WelcomeScreenStandardPreview() {
    LeakOutTheme {
        WelcomeScreen(
            resolveNextLevel = { NextLevelResolver.resolve(totalLevels = 48, stars = emptyMap()) },
            onStartLevel = {},
            onOpenLevelLibrary = {},
            onSettings = {},
            onExit = {},
        )
    }
}

@Preview(name = "Welcome 640x320 (small)", widthDp = 640, heightDp = 320)
@Composable
private fun WelcomeScreenSmallPreview() {
    LeakOutTheme {
        WelcomeScreen(
            resolveNextLevel = { NextLevelResolver.resolve(totalLevels = 48, stars = emptyMap()) },
            onStartLevel = {},
            onOpenLevelLibrary = {},
            onSettings = {},
            onExit = {},
        )
    }
}
