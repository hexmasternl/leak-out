using HexMaster.LeakOut.Players.Abstractions.Services;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>Serves display names to other modules, which only know players by their internal id.</summary>
public sealed class PlayerProfileService(IPlayerRepository playerRepository) : IPlayerProfileService
{
    public async Task<IReadOnlyDictionary<Guid, string>> GetDisplayNamesAsync(
        IReadOnlyCollection<Guid> userIds, CancellationToken ct)
    {
        ArgumentNullException.ThrowIfNull(userIds);
        if (userIds.Count == 0)
        {
            return new Dictionary<Guid, string>();
        }

        return await playerRepository.GetDisplayNamesAsync(userIds.Distinct().ToArray(), ct);
    }
}
