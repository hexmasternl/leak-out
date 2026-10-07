using System.Diagnostics;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Players.Observability;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>
/// Serves display names to other modules, which only know players by their internal id. The names
/// never reach telemetry; a failure is logged by the calling module.
/// </summary>
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

        var distinctIds = userIds.Distinct().ToArray();
        using var activity = PlayersMetrics.ActivitySource.StartActivity(PlayersMetrics.GetDisplayNamesActivityName);
        activity?.SetTag("players.requested", distinctIds.Length);
        try
        {
            var displayNames = await playerRepository.GetDisplayNamesAsync(distinctIds, ct);

            activity?.SetTag("players.found", displayNames.Count);
            activity?.SetStatus(ActivityStatusCode.Ok);
            return displayNames;
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            throw;
        }
    }
}
