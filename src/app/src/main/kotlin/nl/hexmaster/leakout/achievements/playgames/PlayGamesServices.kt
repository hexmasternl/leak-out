package nl.hexmaster.leakout.achievements.playgames

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import nl.hexmaster.leakout.achievements.AchievementRepository

/**
 * Play Games for the app: the sign-in state and the sync of the local achievements.
 *
 * Whether Play Games is used at all is decided by [appId], the `app_id` of `games-ids.xml`. Only a
 * positive number counts as configured; anything else (the `0` placeholder, empty, text) means the
 * SDK is never started, the sign-in state is [SignInState.Unavailable] and nothing is synced, while
 * achievements still work locally.
 *
 * @param initializeSdk starts the Play Games SDK (`PlayGamesSdk.initialize`); only called when configured.
 * @param syncFor the Play Games sync for an activity.
 */
class PlayGamesServices(
    appId: String?,
    achievements: AchievementRepository,
    scope: CoroutineScope,
    private val initializeSdk: () -> Unit,
    gatewayFor: (Activity) -> SignInGateway = ::GmsSignInGateway,
    private val syncFor: (Activity) -> AchievementSync = PlayGamesAchievementSync::of,
    log: PlayGamesLog = PlayGamesLog.Android,
) {
    val configured: Boolean = isConfigured(appId)

    val auth = PlayGamesAuth(configured, scope, gatewayFor, log)

    val syncer = AchievementSyncer(achievements, auth.state, scope, log)

    private var attached: Pair<Activity, AchievementSync>? = null

    /** At process start: starts the SDK when configured, and the syncer, which stays idle until signed in. */
    fun start() {
        if (configured) initializeSdk()
        syncer.start()
    }

    /** When [activity] is created: binds its sync and runs the silent sign-in check. Nothing without a configuration. */
    fun attach(activity: Activity) {
        if (!configured) return
        val sync = syncFor(activity)
        attached = activity to sync
        syncer.bind(sync)
        auth.checkSilently(activity)
    }

    /** When [activity] is destroyed: its Play Games client is no longer used. */
    fun detach(activity: Activity) {
        val (owner, sync) = attached ?: return
        if (owner !== activity) return
        attached = null
        syncer.unbind(sync)
    }

    companion object {
        /** Whether [appId] is a real Play Games app id: a positive number. */
        fun isConfigured(appId: String?): Boolean = appId?.trim()?.toLongOrNull()?.let { it > 0 } == true
    }
}
