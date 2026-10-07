using System.Diagnostics;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Players.Observability;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>
/// Looks up the internal player id for a subject id and caches hits. Misses are not cached, so a
/// player who registered a moment ago resolves on their next request. The subject id never reaches
/// telemetry.
/// </summary>
public sealed partial class UserResolver(
    IPlayerRepository playerRepository,
    IMemoryCache cache,
    PlayersMetrics metrics,
    ILogger<UserResolver> logger) : IUserResolver
{
    public static readonly TimeSpan SlidingExpiration = TimeSpan.FromMinutes(10);

    public async Task<Guid?> ResolveAsync(string subjectId, CancellationToken cancellationToken)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(subjectId);

        using var activity = PlayersMetrics.ActivitySource.StartActivity(PlayersMetrics.ResolveUserActivityName);
        try
        {
            var cacheKey = $"players:subject:{subjectId}";
            if (cache.TryGetValue(cacheKey, out Guid playerId))
            {
                Resolved(activity, PlayersMetrics.ResolutionCacheHit, playerId);
                return playerId;
            }

            var player = await playerRepository.FindBySubjectIdAsync(subjectId, cancellationToken);
            if (player is null)
            {
                Resolved(activity, PlayersMetrics.ResolutionUnknown, null);
                LogSubjectUnknown();
                return null;
            }

            cache.Set(cacheKey, player.Id, new MemoryCacheEntryOptions { SlidingExpiration = SlidingExpiration });
            Resolved(activity, PlayersMetrics.ResolutionCacheMiss, player.Id);
            return player.Id;
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetTag("players.resolution", PlayersMetrics.OutcomeError);
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.UserResolved(PlayersMetrics.OutcomeError);
            LogResolutionFailed(ex);
            throw;
        }
    }

    private void Resolved(Activity? activity, string resolution, Guid? playerId)
    {
        activity?.SetTag("players.resolution", resolution);
        if (playerId is not null)
        {
            activity?.SetTag("player.id", playerId.Value);
        }

        activity?.SetStatus(ActivityStatusCode.Ok);
        metrics.UserResolved(resolution);
    }

    [LoggerMessage(Level = LogLevel.Debug, Message = "Access token subject did not resolve to a registered player")]
    private partial void LogSubjectUnknown();

    [LoggerMessage(Level = LogLevel.Error, Message = "Resolving the access token subject failed unexpectedly")]
    private partial void LogResolutionFailed(Exception exception);
}
