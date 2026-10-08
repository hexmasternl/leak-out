namespace HexMaster.LeakOut.Players.Abstractions.Services;

/// <summary>
/// Resolves the subject id of an access token (the Google Play Games player id) to the internal
/// player id. Used at the API boundary only, before any command or query is dispatched.
/// </summary>
public interface IUserResolver
{
    /// <returns>The internal player id, or <see langword="null"/> when no player is registered for the subject id.</returns>
    Task<Guid?> ResolveAsync(string subjectId, CancellationToken cancellationToken);
}
