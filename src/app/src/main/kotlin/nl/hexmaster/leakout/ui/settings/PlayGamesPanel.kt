package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Test tags of the Play Games panel. */
object PlayGamesPanelTags {
    const val PANEL = "settings_play_games_panel"
    const val STATUS = "settings_play_games_status"
    const val SIGN_IN = "settings_play_games_sign_in"
    const val ACHIEVEMENTS = "settings_achievements"
}

/**
 * The Play Games panel (design.md decision 7): the sign-in state, a glass Sign in button while
 * signed out (disabled while signing in, gone when signed in or when Play Games is unavailable),
 * and the always-present Achievements button with the unlocked count, because the achievements
 * screen works from the local data.
 */
@Composable
fun PlayGamesPanel(
    state: PlayGamesUiState,
    onSignIn: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPanel(title = stringResource(R.string.settings_play_games), modifier = modifier.testTag(PlayGamesPanelTags.PANEL)) {
        Spacer(Modifier.height(12.dp))
        val signIn = state.signIn
        val status = when (signIn) {
            is SignInState.SignedIn -> if (signIn.displayName.isBlank()) {
                stringResource(R.string.settings_play_games_signed_in_anonymous)
            } else {
                stringResource(R.string.settings_play_games_signed_in, signIn.displayName)
            }
            SignInState.SignedOut -> stringResource(R.string.settings_play_games_signed_out)
            SignInState.SigningIn -> stringResource(R.string.settings_play_games_signing_in)
            SignInState.Unavailable -> stringResource(R.string.settings_play_games_unavailable)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (signIn is SignInState.SignedIn) LeakOutColors.Success500 else LeakOutColors.TextMuted, CircleShape),
            )
            Text(
                text = status,
                style = LeakOutTextStyles.BodyMd,
                color = LeakOutColors.TextPrimary,
                modifier = Modifier.testTag(PlayGamesPanelTags.STATUS),
            )
        }
        if (signIn == SignInState.SignedOut || signIn == SignInState.SigningIn) {
            Spacer(Modifier.height(12.dp))
            FlowButton(
                text = stringResource(R.string.settings_play_games_sign_in),
                onClick = onSignIn,
                variant = FlowButtonVariant.Secondary,
                size = FlowButtonSize.Medium,
                enabled = signIn == SignInState.SignedOut,
                modifier = Modifier.fillMaxWidth().testTag(PlayGamesPanelTags.SIGN_IN),
            )
        }
        Spacer(Modifier.height(12.dp))
        val description = stringResource(R.string.settings_achievements_description, state.unlocked, state.total)
        FlowButton(
            text = stringResource(R.string.settings_achievements),
            trailing = stringResource(R.string.settings_achievements_count, state.unlocked, state.total),
            onClick = onOpenAchievements,
            variant = FlowButtonVariant.Secondary,
            size = FlowButtonSize.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PlayGamesPanelTags.ACHIEVEMENTS)
                .semantics { contentDescription = description },
        )
    }
}

@Preview(name = "Play Games panel: all states", widthDp = 360, heightDp = 760, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun PlayGamesPanelPreview() {
    LeakOutTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (state in listOf(SignInState.SignedIn("Eduard"), SignInState.SignedOut, SignInState.SigningIn, SignInState.Unavailable)) {
                PlayGamesPanel(PlayGamesUiState(state, 12, 25), onSignIn = {}, onOpenAchievements = {})
            }
        }
    }
}
