namespace HexMaster.LeakOut.Players.Services;

/// <summary>Verifies a Google Play Games server auth code with Google.</summary>
public interface IGooglePlayGamesClient
{
    /// <summary>
    /// Exchanges the one-time server auth code and returns the player it belongs to.
    /// </summary>
    /// <exception cref="GooglePlayGamesAuthenticationException">Google rejected the code.</exception>
    /// <exception cref="GooglePlayGamesUnavailableException">Google could not be reached or failed.</exception>
    Task<GooglePlayGamesPlayer> GetPlayerAsync(string serverAuthCode, CancellationToken cancellationToken);
}
