package nl.hexmaster.leakout.scores.online

import nl.hexmaster.leakout.BuildConfig

/**
 * Where scores go: the score server's [baseUrl] and the web OAuth client id the Play Games server
 * auth code is requested for ([webClientId], the same id the server exchanges codes with). Score
 * posting is on only when both are set and the URL is `https://`, so cleartext traffic never happens.
 */
data class ScoreServerConfig(val baseUrl: String, val webClientId: String) {
    val enabled: Boolean = webClientId.isNotBlank() && baseUrl.trim().startsWith("https://") && baseUrl.trim().length > "https://".length

    companion object {
        /** The values the build was made with (`leakout.scoreServerUrl` / `leakout.scoreServerClientId`). */
        fun fromBuildConfig(): ScoreServerConfig =
            ScoreServerConfig(BuildConfig.SCORE_SERVER_URL, BuildConfig.SCORE_SERVER_CLIENT_ID)
    }
}
