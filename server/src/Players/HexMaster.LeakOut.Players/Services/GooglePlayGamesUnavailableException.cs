namespace HexMaster.LeakOut.Players.Services;

/// <summary>Google could not be reached or answered with an error that is not the caller's fault.</summary>
public sealed class GooglePlayGamesUnavailableException : Exception
{
    public GooglePlayGamesUnavailableException(string message)
        : base(message)
    {
    }

    public GooglePlayGamesUnavailableException(string message, Exception innerException)
        : base(message, innerException)
    {
    }
}
