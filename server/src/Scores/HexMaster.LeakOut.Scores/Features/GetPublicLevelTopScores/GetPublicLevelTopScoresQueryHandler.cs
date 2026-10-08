using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;

/// <summary>
/// Returns the 25 best scores of a level in ranking order with the players' display names, without
/// player ids. Each level's result is kept in memory for <see cref="CacheDuration"/>, so anonymous
/// traffic reaches the database at most once per level in that time.
/// </summary>
public sealed partial class GetPublicLevelTopScoresQueryHandler(
    ILevelScoreRepository repository,
    IPlayerProfileService playerProfiles,
    IMemoryCache cache,
    ScoresMetrics metrics,
    ILogger<GetPublicLevelTopScoresQueryHandler> logger) : IQueryHandler<GetPublicLevelTopScoresQuery, PublicLevelLeaderboardDto>
{
    public const int TopCount = 25;

    public static readonly TimeSpan CacheDuration = TimeSpan.FromMinutes(30);

    public async Task<PublicLevelLeaderboardDto> Handle(GetPublicLevelTopScoresQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var started = Stopwatch.GetTimestamp();
        using var activity = ScoresMetrics.ActivitySource.StartActivity(ScoresMetrics.GetPublicLevelTopScoresActivityName);
        activity?.SetTag("score.level", query.Level);
        activity?.SetTag("leaderboard.kind", ScoresMetrics.LeaderboardKindPublicTop);
        try
        {
            var cacheKey = CacheKey(query.Level);
            var cacheHit = cache.TryGetValue(cacheKey, out PublicLevelLeaderboardDto? cached) && cached is not null;
            PublicLevelLeaderboardDto leaderboard;
            if (cacheHit)
            {
                leaderboard = cached!;
            }
            else
            {
                leaderboard = await BuildLeaderboardAsync(query.Level, cancellationToken);
                // Only a successful read is cached; a failure throws before this line.
                cache.Set(cacheKey, leaderboard, new MemoryCacheEntryOptions { AbsoluteExpirationRelativeToNow = CacheDuration });
            }

            activity?.SetTag("leaderboard.cache_hit", cacheHit);
            activity?.SetTag("leaderboard.entries", leaderboard.Entries.Count);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.LeaderboardServed(ScoresMetrics.LeaderboardKindPublicTop, Stopwatch.GetElapsedTime(started));
            return leaderboard;
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.LeaderboardFailed(ScoresMetrics.LeaderboardKindPublicTop, Stopwatch.GetElapsedTime(started));
            LogLeaderboardFailed(ex, ScoresMetrics.LeaderboardKindPublicTop);
            throw;
        }
    }

    /// <summary>The memory cache key of a level's public top scores.</summary>
    public static string CacheKey(int level) => $"scores:public-top:{level}";

    private async Task<PublicLevelLeaderboardDto> BuildLeaderboardAsync(int level, CancellationToken cancellationToken)
    {
        var ranked = await repository.GetRankedAsync(level, 0, TopCount, cancellationToken);
        if (ranked.Count == 0)
        {
            return new PublicLevelLeaderboardDto(level, []);
        }

        var displayNames = await playerProfiles.GetDisplayNamesAsync(
            ranked.Select(s => s.UserId).ToArray(), cancellationToken);

        var entries = ranked
            .Select(s => new PublicLeaderboardEntryDto(
                s.Position,
                displayNames.TryGetValue(s.UserId, out var name) && !string.IsNullOrWhiteSpace(name)
                    ? name
                    : GetLevelTopScoresQueryHandler.UnknownPlayerName,
                s.Score))
            .ToList();

        return new PublicLevelLeaderboardDto(level, entries);
    }

    [LoggerMessage(Level = LogLevel.Error, Message = "{LeaderboardKind} leaderboard request failed unexpectedly")]
    private partial void LogLeaderboardFailed(Exception exception, string leaderboardKind);
}
