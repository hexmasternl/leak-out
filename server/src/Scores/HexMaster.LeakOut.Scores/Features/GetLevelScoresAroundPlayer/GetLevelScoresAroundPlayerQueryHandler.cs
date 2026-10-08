using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;

/// <summary>
/// Returns up to 10 ranked scores of a level around the requesting player's best score: 5 above it,
/// the player's own and 4 below, shifted at the edges of the ranking. Empty when the player has no
/// score on the level.
/// </summary>
public sealed partial class GetLevelScoresAroundPlayerQueryHandler(
    ILevelScoreRepository repository,
    IPlayerProfileService playerProfiles,
    ScoresMetrics metrics,
    ILogger<GetLevelScoresAroundPlayerQueryHandler> logger) : IQueryHandler<GetLevelScoresAroundPlayerQuery, LevelLeaderboardDto>
{
    public const string UnknownPlayerName = "Unknown player";

    public async Task<LevelLeaderboardDto> Handle(GetLevelScoresAroundPlayerQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var started = Stopwatch.GetTimestamp();
        using var activity = ScoresMetrics.ActivitySource.StartActivity(ScoresMetrics.GetLevelScoresAroundPlayerActivityName);
        activity?.SetTag("player.id", query.UserId);
        activity?.SetTag("score.level", query.Level);
        activity?.SetTag("leaderboard.kind", ScoresMetrics.LeaderboardKindAroundPlayer);
        try
        {
            var leaderboard = await BuildLeaderboardAsync(query, activity, cancellationToken);

            activity?.SetTag("leaderboard.entries", leaderboard.Entries.Count);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.LeaderboardServed(ScoresMetrics.LeaderboardKindAroundPlayer, Stopwatch.GetElapsedTime(started));
            return leaderboard;
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.LeaderboardFailed(ScoresMetrics.LeaderboardKindAroundPlayer, Stopwatch.GetElapsedTime(started));
            LogLeaderboardFailed(ex, ScoresMetrics.LeaderboardKindAroundPlayer);
            throw;
        }
    }

    private async Task<LevelLeaderboardDto> BuildLeaderboardAsync(
        GetLevelScoresAroundPlayerQuery query, Activity? activity, CancellationToken cancellationToken)
    {
        var best = await repository.GetBestAsync(query.UserId, query.Level, cancellationToken);
        activity?.SetTag("player.has_score", best is not null);
        if (best is null)
        {
            return new LevelLeaderboardDto(query.Level, []);
        }

        var rankedAbove = await repository.CountRankedAboveAsync(
            query.Level, best.Score, best.AchievedAt, query.UserId, cancellationToken);
        var total = await repository.CountAsync(query.Level, cancellationToken);

        // A score submitted between the reads can only push the total up, so the player's position always fits in it.
        var window = LeaderboardWindow.Compute(rankedAbove + 1, Math.Max(total, rankedAbove + 1));
        var ranked = await repository.GetRankedAsync(query.Level, window.Start - 1, window.Count, cancellationToken);

        var displayNames = await playerProfiles.GetDisplayNamesAsync(
            ranked.Select(s => s.UserId).ToArray(), cancellationToken);

        var entries = ranked
            .Select(s => new LeaderboardEntryDto(
                s.Position,
                displayNames.TryGetValue(s.UserId, out var name) && !string.IsNullOrWhiteSpace(name) ? name : UnknownPlayerName,
                s.Score,
                s.UserId == query.UserId))
            .ToList();

        return new LevelLeaderboardDto(query.Level, entries);
    }

    [LoggerMessage(Level = LogLevel.Error, Message = "{LeaderboardKind} leaderboard request failed unexpectedly")]
    private partial void LogLeaderboardFailed(Exception exception, string leaderboardKind);
}
