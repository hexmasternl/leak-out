namespace HexMaster.LeakOut.Players.Abstractions.Services;

/// <summary>
/// Reads public player profile data by internal player id, for modules that store players only by
/// that id and need to show who they are.
/// </summary>
public interface IPlayerProfileService
{
    /// <summary>Looks up the display names of the given players in one batch.</summary>
    /// <returns>The display name per internal player id; ids of unknown players are absent.</returns>
    Task<IReadOnlyDictionary<Guid, string>> GetDisplayNamesAsync(IReadOnlyCollection<Guid> userIds, CancellationToken ct);
}
