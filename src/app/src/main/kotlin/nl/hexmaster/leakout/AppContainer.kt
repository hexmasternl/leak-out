package nl.hexmaster.leakout

import android.content.Context
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CoroutineScope
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.data.RoomAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.PlayGamesServices
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository
import nl.hexmaster.leakout.scores.online.CallbackNetworkMonitor
import nl.hexmaster.leakout.scores.online.DataStorePendingScoreQueue
import nl.hexmaster.leakout.scores.online.HttpScoreServerApi
import nl.hexmaster.leakout.scores.online.ScoreServerConfig
import nl.hexmaster.leakout.scores.online.ScoreSubmitter
import nl.hexmaster.leakout.scores.online.scoreQueueDataStore

/**
 * The app's long-lived objects, made once per process by [LeakOutApplication] and handed to the
 * activity and its view models by hand: a handful of objects does not need a DI framework.
 *
 * @param scope lives as long as the process; work that must outlive a screen runs in it.
 * @param playGamesAppId the Play Games app id (`R.string.app_id`); not a positive number means no Play Games.
 */
class AppContainer(context: Context, val scope: CoroutineScope, playGamesAppId: String?) {
    private val appContext = context.applicationContext

    private val database: LeakOutDatabase by lazy { LeakOutDatabase.get(appContext) }

    /** The player's finished levels, kept on the device across restarts. */
    val history: LevelHistoryRepository by lazy { RoomLevelHistoryRepository(database) }

    /** The local achievement store, the source of truth for achievements. */
    val achievements: AchievementRepository by lazy { RoomAchievementRepository(database) }

    /** Decides and stores achievements when an attempt ends, is abandoned, or at start-up. */
    val tracker: AchievementTracker by lazy { AchievementTracker(achievements, history, scope) }

    /** Play Games sign-in ([PlayGamesServices.auth]) and the sync of [achievements] to it. */
    val playGames: PlayGamesServices by lazy {
        PlayGamesServices(playGamesAppId, achievements, scope, initializeSdk = { PlayGamesSdk.initialize(appContext) })
    }

    /** Sends the scores of won levels to the score server, through a queue kept on the device. */
    val scores: ScoreSubmitter by lazy {
        val config = ScoreServerConfig.fromBuildConfig()
        ScoreSubmitter(
            config = config,
            api = HttpScoreServerApi(config.baseUrl),
            queue = DataStorePendingScoreQueue(appContext.scoreQueueDataStore),
            signIn = playGames.auth.state,
            serverAuthCodes = playGames.serverAuthCodes,
            network = CallbackNetworkMonitor.android(appContext),
            scope = scope,
        )
    }
}

/** The process's [AppContainer]. */
val Context.appContainer: AppContainer get() = (applicationContext as LeakOutApplication).container
