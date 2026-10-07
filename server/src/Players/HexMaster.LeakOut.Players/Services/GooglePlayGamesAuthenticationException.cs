namespace HexMaster.LeakOut.Players.Services;

/// <summary>Google rejected the server auth code: it is invalid, expired or already used.</summary>
public sealed class GooglePlayGamesAuthenticationException : Exception
{
    public GooglePlayGamesAuthenticationException(string message)
        : base(message)
    {
    }

    public GooglePlayGamesAuthenticationException(string message, Exception innerException)
        : base(message, innerException)
    {
    }
}
