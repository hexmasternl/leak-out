package nl.hexmaster.leakout.achievements.playgames

import nl.hexmaster.leakout.core.achievements.Achievement

/** Records every call in order; [failing] makes the calls for those achievements throw, like an offline device. */
class RecordingAchievementSync(var failing: Set<Achievement> = emptySet()) : AchievementSync {
    val calls = mutableListOf<String>()

    override suspend fun unlock(achievement: Achievement) {
        calls += "unlock ${achievement.key}"
        if (achievement in failing) throw java.io.IOException("offline")
    }

    override suspend fun setSteps(achievement: Achievement, steps: Int) {
        calls += "setSteps ${achievement.key} $steps"
        if (achievement in failing) throw java.io.IOException("offline")
    }
}

/** A scripted Play Games sign-in. */
class FakeSignInGateway(
    var authenticated: Boolean = false,
    var signInSucceeds: Boolean = true,
    var name: String? = "Eduard",
    var throwOnSignIn: Boolean = false,
) : SignInGateway {
    var signInCalls = 0

    override suspend fun isAuthenticated(): Boolean = authenticated

    override suspend fun signIn(): Boolean {
        signInCalls++
        if (throwOnSignIn) throw IllegalStateException("sign-in failed")
        authenticated = signInSucceeds
        return signInSucceeds
    }

    override suspend fun displayName(): String? = name
}

/** Keeps logged failures instead of calling android.util.Log, which JVM tests cannot. */
class RecordingLog : PlayGamesLog {
    val failures = mutableListOf<String>()

    override fun failed(what: String, error: Throwable) {
        failures += what
    }
}
