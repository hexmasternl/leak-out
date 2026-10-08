package nl.hexmaster.leakout.achievements.playgames

import android.app.Activity
import com.google.android.gms.games.PlayGames

/** Hands out one-time Play Games server auth codes, which a game server exchanges to identify the player. */
fun interface ServerAuthCodeSource {
    /** A fresh code requested for the server's web OAuth client [webClientId]; throws when Play Games refuses. */
    suspend fun serverAuthCode(webClientId: String): String
}

/** [ServerAuthCodeSource] through the Play Games sign-in client of [activity]. */
class GmsServerAuthCodeSource(private val activity: Activity) : ServerAuthCodeSource {
    // No refresh token: the server only needs to know who the player is, once per session.
    override suspend fun serverAuthCode(webClientId: String): String =
        PlayGames.getGamesSignInClient(activity).requestServerSideAccess(webClientId, false).await()
}
