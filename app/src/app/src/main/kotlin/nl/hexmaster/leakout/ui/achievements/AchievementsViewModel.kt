package nl.hexmaster.leakout.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AchievementType

/** How one card on the achievements screen looks. */
enum class CardLook {
    /** Colour icon, unlock date, `success.tint` wash. */
    Unlocked,

    /** Dimmed, desaturated icon; incremental ones show their progress. */
    Locked,

    /** A locked hidden achievement: the "?" medallion, "Hidden achievement", no real name. */
    HiddenLocked,
}

/**
 * One card of the achievements screen, from the local [progress].
 *
 * @param progress the steps to show as "x / y" with a bar, only for a locked incremental achievement.
 */
data class AchievementCard(
    val achievement: Achievement,
    val look: CardLook,
    val progress: Int?,
    val steps: Int?,
    val unlockedAtMs: Long?,
) {
    /** The bar's filled share, 0 to 1; 0 without progress. */
    val fraction: Float get() = if (progress == null || steps == null || steps == 0) 0f else (progress.toFloat() / steps).coerceIn(0f, 1f)

    companion object {
        fun of(progress: AchievementProgress): AchievementCard {
            val achievement = progress.achievement
            val look = when {
                progress.unlocked -> CardLook.Unlocked
                achievement.hidden -> CardLook.HiddenLocked
                else -> CardLook.Locked
            }
            val incremental = achievement.type as? AchievementType.Incremental
            val shown = incremental?.takeIf { look == CardLook.Locked }
            return AchievementCard(
                achievement = achievement,
                look = look,
                progress = shown?.let { progress.steps.coerceIn(0, it.steps) },
                steps = shown?.steps,
                unlockedAtMs = progress.unlockedAtMs,
            )
        }
    }
}

/** The texts of a card, as plain functions so they are JVM-testable. */
object AchievementCardText {
    /** The unlock date in the device's medium date style, in [zone]. */
    fun date(atMs: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(Instant.ofEpochMilli(atMs).atZone(zone))

    /**
     * What a screen reader reads for one card: the [name], [state] ("locked"/"unlocked"), the
     * [progress] ("370 of 1000") when there is one, then the [description] and the [date] line.
     */
    fun spoken(name: String, state: String, progress: String?, description: String, date: String?): String =
        listOfNotNull(name, state, progress, description, date).joinToString(", ")
}

/** What the achievements screen shows. */
data class AchievementsUiState(val cards: List<AchievementCard>, val signedIn: Boolean) {
    val unlocked: Int get() = cards.count { it.look == CardLook.Unlocked }
    val total: Int get() = cards.size

    companion object {
        fun of(progress: List<AchievementProgress>, signIn: SignInState): AchievementsUiState =
            AchievementsUiState(progress.map(AchievementCard::of), signIn is SignInState.SignedIn)
    }
}

/**
 * The achievements screen's state: every catalogue achievement from the local store, in catalogue
 * order, whether or not the player is signed in; [signIn] only decides the Play Games button.
 * Null until the store has been read once.
 */
class AchievementsViewModel(
    achievements: AchievementRepository,
    signIn: StateFlow<SignInState>,
) : ViewModel() {
    val uiState: StateFlow<AchievementsUiState?> =
        combine(achievements.observeAll(), signIn, AchievementsUiState::of)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
