package nl.hexmaster.leakout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Material components inherit the Leak Out look through this mapping; custom components read
// LeakOutColors directly.
private val LeakOutColorScheme = darkColorScheme(
    primary = LeakOutColors.PrimaryBase,
    onPrimary = LeakOutColors.SurfaceCard,
    primaryContainer = LeakOutColors.PrimaryDark,
    onPrimaryContainer = LeakOutColors.PrimarySoft,
    secondary = LeakOutColors.SecondaryBase,
    onSecondary = LeakOutColors.InkDark,
    secondaryContainer = LeakOutColors.SecondaryDark,
    onSecondaryContainer = LeakOutColors.SecondaryBg,
    error = LeakOutColors.ErrorRed,
    onError = LeakOutColors.SurfaceCard,
    background = LeakOutColors.SceneNavy,
    onBackground = LeakOutColors.SurfaceCard,
    surface = LeakOutColors.SceneNavy,
    onSurface = LeakOutColors.SurfaceCard,
    outline = LeakOutColors.InkDark,
)

@Composable
fun LeakOutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeakOutColorScheme,
        typography = LeakOutTypography,
        content = content,
    )
}
