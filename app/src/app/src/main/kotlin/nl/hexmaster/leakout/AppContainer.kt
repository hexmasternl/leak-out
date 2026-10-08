package nl.hexmaster.leakout

import android.content.Context
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.data.RoomAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.PlayGamesServices
import nl.hexmaster.leakout.core.update.UpdateGate
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository
import nl.hexmaster.leakout.scores.online.CallbackNetworkMonitor
import nl.hexmaster.leakout.scores.online.DataStorePendingScoreQueue
import nl.hexmaster.leakout.scores.online.HttpScoreServerApi
import nl.hexmaster.leakout.scores.online.ScoreServerConfig
import nl.hexmaster.leakout.scores.online.ScoreServerSession
import nl.hexmaster.leakout.scores.online.ScoreSubmitter
import nl.hexmaster.leakout.scores.online.TopScoresLoader
import nl.hexmaster.leakout.scores.online.scoreQueueDataStore
import nl.hexmaster.leakout.update.AppUpdates
import nl.hexmaster.leakout.update.PlayAppUpdates

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

    /** The score server this build talks to; [ScoreServerConfig.enabled] is false when it has none. */
    val scoreServerConfig: ScoreServerConfig by lazy { ScoreServerConfig.fromBuildConfig() }

    private val scoreServerApi: HttpScoreServerApi by lazy { HttpScoreServerApi(scoreServerConfig.baseUrl) }

    // One sign-in to the score server for posting scores and loading the top 10.
    private val scoreServerSession: ScoreServerSession by lazy { ScoreServerSession(scoreServerApi, scoreServerConfig.webClientId) }

    /** Sends the scores of won levels to the score server, through a queue kept on the device. */
    val scores: ScoreSubmitter by lazy {
        ScoreSubmitter(
            config = scoreServerConfig,
            api = scoreServerApi,
            queue = DataStorePendingScoreQueue(appContext.scoreQueueDataStore),
            signIn = playGames.auth.state,
            serverAuthCodes = playGames.serverAuthCodes,
            network = CallbackNetworkMonitor.android(appContext),
            scope = scope,
            session = scoreServerSession,
        )
    }

    /** Loads the global top 10 of a level from the score server. */
    val topScores: TopScoresLoader by lazy {
        TopScoresLoader(scoreServerApi, scoreServerSession, playGames.auth.state, playGames.serverAuthCodes)
    }

    /** Google Play In-App Updates: whether a newer version is available and how urgent it is. */
    val appUpdates: AppUpdates by lazy { PlayAppUpdates.create(appContext) }

    /** What the update screens show, kept per process so the optional prompt is offered only once. */
    val updateGate = MutableStateFlow(UpdateGate())
}

/** The process's [AppContainer]. */
val Context.appContainer: AppContainer get() = (applicationContext as LeakOutApplication).container
