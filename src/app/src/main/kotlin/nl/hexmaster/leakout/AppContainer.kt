package nl.hexmaster.leakout

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.data.RoomAchievementRepository
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository

/**
 * The app's long-lived objects, made once per process by [LeakOutApplication] and handed to the
 * activity and its view models by hand: a handful of objects does not need a DI framework.
 *
 * @param scope lives as long as the process; work that must outlive a screen runs in it.
 */
class AppContainer(context: Context, val scope: CoroutineScope) {
    private val appContext = context.applicationContext

    private val database: LeakOutDatabase by lazy { LeakOutDatabase.get(appContext) }

    /** The player's finished levels, kept on the device across restarts. */
    val history: LevelHistoryRepository by lazy { RoomLevelHistoryRepository(database) }

    /** The local achievement store, the source of truth for achievements. */
    val achievements: AchievementRepository by lazy { RoomAchievementRepository(database) }

    /** Decides and stores achievements when an attempt ends, is abandoned, or at start-up. */
    val tracker: AchievementTracker by lazy { AchievementTracker(achievements, history, scope) }
}

/** The process's [AppContainer]. */
val Context.appContainer: AppContainer get() = (applicationContext as LeakOutApplication).container
