package nl.hexmaster.leakout.ui.achievements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementResources
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.settings.SettingsIconButton
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object AchievementsTags {
    const val SCREEN = "achievements_screen"
    const val TITLE = "achievements_title"
    const val BACK = "achievements_back"
    const val COUNT = "achievements_count"
    const val PLAY_GAMES = "achievements_play_games"
    const val GRID = "achievements_grid"
    const val PROGRESS_BAR = "achievement_progress_bar"
}

/** The tag of the card of [achievement]. */
fun achievementCardTag(achievement: Achievement) = "achievement_card_${achievement.key}"

/** Sizes of the achievements screen (design.md decision 8). */
object AchievementsMetrics {
    val IconSize: Dp = 64.dp
    val CardMinWidth: Dp = 280.dp
    val CardRadius: Dp = 16.dp
    val Gutter: Dp = 16.dp
    val BarHeight: Dp = 8.dp

    /** Locked icons are drawn at 40 %, desaturated. */
    const val LOCKED_ICON_ALPHA = 0.4f
}

/**
 * The achievements screen bound to [viewModel]. The back button and system Back leave through
 * [onBack]; the Play Games button, shown only while signed in, calls [onOpenPlayGames].
 */
@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    onBack: () -> Unit,
    onOpenPlayGames: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AchievementsContent(state, onBack, onOpenPlayGames, modifier)
}

/**
 * Stateless achievements screen: a top bar with the back button, the title, the unlocked count
 * and (signed in only) the Play Games button, then a scrolling grid of level 2 cards in catalogue
 * order. Until [state] has been read only the top bar shows.
 */
@Composable
fun AchievementsContent(
    state: AchievementsUiState?,
    onBack: () -> Unit,
    onOpenPlayGames: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = AchievementsMetrics.Gutter)
            .testTag(AchievementsTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(AchievementsMetrics.Gutter),
    ) {
        TopBar(state, onBack, onOpenPlayGames)
        if (state == null) return@Column
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AchievementsMetrics.CardMinWidth),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(AchievementsTags.GRID),
            horizontalArrangement = Arrangement.spacedBy(AchievementsMetrics.Gutter),
            verticalArrangement = Arrangement.spacedBy(AchievementsMetrics.Gutter),
            contentPadding = PaddingValues(bottom = AchievementsMetrics.Gutter),
        ) {
            items(state.cards, key = { it.achievement.key }) { card -> AchievementCardView(card) }
        }
    }
}

@Composable
private fun TopBar(state: AchievementsUiState?, onBack: () -> Unit, onOpenPlayGames: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AchievementsMetrics.Gutter),
    ) {
        SettingsIconButton(
            icon = R.drawable.ic_back,
            description = stringResource(R.string.achievements_back),
            onClick = onBack,
            modifier = Modifier.testTag(AchievementsTags.BACK),
        )
        Text(
            text = stringResource(R.string.achievements_title).displayUppercase(),
            style = LeakOutTextStyles.DisplayLg,
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.testTag(AchievementsTags.TITLE).semantics { heading() },
        )
        if (state != null) {
            val description = pluralStringResource(R.plurals.achievements_count_description, state.total, state.unlocked, state.total)
            Text(
                text = stringResource(R.string.achievements_count, state.unlocked, state.total),
                style = LeakOutTextStyles.NumericMd,
                color = LeakOutColors.Brass300,
                modifier = Modifier.testTag(AchievementsTags.COUNT).semantics { contentDescription = description },
            )
        }
        Spacer(Modifier.weight(1f))
        if (state?.signedIn == true) {
            FlowButton(
                text = stringResource(R.string.achievements_play_games),
                onClick = onOpenPlayGames,
                variant = FlowButtonVariant.Secondary,
                size = FlowButtonSize.Medium,
                modifier = Modifier.testTag(AchievementsTags.PLAY_GAMES),
            )
        }
    }
}

private val Desaturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * One level 2 card: the icon, the name in `display.sm`, the description in `body.sm`, then the
 * unlock date or, for a locked incremental achievement, the progress bar with "x / y". Read as one
 * item: name, locked or unlocked, the progress, the description and the date.
 */
@Composable
private fun AchievementCardView(card: AchievementCard) {
    val shape = RoundedCornerShape(AchievementsMetrics.CardRadius)
    val hiddenLocked = card.look == CardLook.HiddenLocked
    val name = if (hiddenLocked) stringResource(R.string.achievements_hidden_name) else stringResource(AchievementResources.name(card.achievement))
    val description = if (hiddenLocked) {
        stringResource(R.string.achievements_hidden_description)
    } else {
        stringResource(AchievementResources.description(card.achievement))
    }
    val locale = LocalConfiguration.current.locales[0]
    val date = card.unlockedAtMs?.takeIf { card.look == CardLook.Unlocked }?.let { at ->
        val formatted = remember(at, locale) { AchievementCardText.date(at, locale) }
        stringResource(R.string.achievements_unlocked_on, formatted)
    }
    val state = stringResource(if (card.look == CardLook.Unlocked) R.string.achievements_state_unlocked else R.string.achievements_state_locked)
    val spokenProgress = if (card.progress != null && card.steps != null) {
        stringResource(R.string.achievements_progress_description, card.progress, card.steps)
    } else {
        null
    }
    val spoken = AchievementCardText.spoken(name, state, spokenProgress, description, date)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(LeakOutColors.SurfaceRaised, shape)
            .then(if (card.look == CardLook.Unlocked) Modifier.background(LeakOutColors.SuccessTint, shape) else Modifier)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .padding(AchievementsMetrics.Gutter)
            .testTag(achievementCardTag(card.achievement))
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(AchievementsMetrics.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AchievementIcon(card)
        Column(Modifier.weight(1f)) {
            Text(name, style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(description, style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextSecondary)
            if (date != null) {
                Spacer(Modifier.height(4.dp))
                Text(date, style = LeakOutTextStyles.LabelMd, color = LeakOutColors.Success500)
            }
            if (card.progress != null && card.steps != null) {
                Spacer(Modifier.height(8.dp))
                ProgressRow(card)
            }
        }
    }
}

@Composable
private fun AchievementIcon(card: AchievementCard) {
    val iconShape = RoundedCornerShape(12.dp)
    val modifier = Modifier.size(AchievementsMetrics.IconSize).clip(iconShape)
    when (card.look) {
        CardLook.Unlocked -> Image(painterResource(AchievementResources.icon(card.achievement)), contentDescription = null, modifier = modifier)
        CardLook.Locked -> Image(
            painterResource(AchievementResources.icon(card.achievement)),
            contentDescription = null,
            colorFilter = Desaturate,
            modifier = modifier.alpha(AchievementsMetrics.LOCKED_ICON_ALPHA),
        )
        CardLook.HiddenLocked -> Image(painterResource(AchievementResources.HIDDEN_ICON), contentDescription = null, modifier = modifier)
    }
}

/** The `liquid.500` bar on a `line.subtle` pill track, with "x / y" after it. */
@Composable
private fun ProgressRow(card: AchievementCard) {
    val progress = card.progress ?: return
    val steps = card.steps ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val pill = RoundedCornerShape(percent = 50)
        Box(
            Modifier
                .weight(1f)
                .height(AchievementsMetrics.BarHeight)
                .clip(pill)
                .background(LeakOutColors.LineSubtle)
                .testTag(AchievementsTags.PROGRESS_BAR),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(card.fraction).background(LeakOutColors.Liquid500, pill))
        }
        Text(
            text = stringResource(R.string.achievements_progress, progress, steps),
            style = LeakOutTextStyles.LabelMd,
            color = LeakOutColors.TextSecondary,
            modifier = Modifier.width(IntrinsicProgressWidth),
            maxLines = 1,
        )
    }
}

// Room for "9999 / 10000", so bars in one column line up.
private val IntrinsicProgressWidth: Dp = 96.dp

private val PreviewState = AchievementsUiState.of(
    Achievement.entries.map { achievement ->
        when (achievement) {
            Achievement.FIRST_LEAK_FIXED, Achievement.WET_FEET, Achievement.PHOTO_FINISH ->
                AchievementProgress(achievement, unlockedAtMs = 1_790_000_000_000)
            Achievement.JOURNEYMAN -> AchievementProgress(achievement, steps = 370)
            Achievement.STAR_COLLECTOR_1 -> AchievementProgress(achievement, steps = 18)
            else -> AchievementProgress(achievement)
        }
    },
    SignInState.SignedIn("Eduard"),
)

@Preview(name = "Achievements 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun AchievementsPreview() {
    LeakOutTheme { AchievementsContent(PreviewState, onBack = {}, onOpenPlayGames = {}) }
}

@Preview(name = "Achievements 640x320 (small), signed out", widthDp = 640, heightDp = 320)
@Composable
private fun AchievementsSmallPreview() {
    LeakOutTheme { AchievementsContent(PreviewState.copy(signedIn = false), onBack = {}, onOpenPlayGames = {}) }
}
