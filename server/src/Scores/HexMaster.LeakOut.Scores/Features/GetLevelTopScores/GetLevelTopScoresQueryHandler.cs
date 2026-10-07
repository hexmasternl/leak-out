using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.Scores.Features.GetLevelTopScores;

/// <summary>
/// Returns the 10 best scores of a level in ranking order, with the players' display names and the
/// requesting player's entry marked.
/// </summary>
public sealed partial class GetLevelTopScoresQueryHandler(
    ILevelScoreRepository repository,
    IPlayerProfileService playerProfiles,
    ScoresMetrics metrics,
    ILogger<GetLevelTopScoresQueryHandler> logger) : IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto>
{
    public const int TopCount = 10;
    public const string UnknownPlayerName = "Unknown player";

    public async Task<LevelLeaderboardDto> Handle(GetLevelTopScoresQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var started = Stopwatch.GetTimestamp();
        using var activity = ScoresMetrics.ActivitySource.StartActivity(ScoresMetrics.GetLevelTopScoresActivityName);
        activity?.SetTag("player.id", query.UserId);
        activity?.SetTag("score.level", query.Level);
        activity?.SetTag("leaderboard.kind", ScoresMetrics.LeaderboardKindTop);
        try
        {
            var leaderboard = await BuildLeaderboardAsync(query, cancellationToken);

            activity?.SetTag("leaderboard.entries", leaderboard.Entries.Count);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.LeaderboardServed(ScoresMetrics.LeaderboardKindTop, Stopwatch.GetElapsedTime(started));
            return leaderboard;
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.LeaderboardFailed(ScoresMetrics.LeaderboardKindTop, Stopwatch.GetElapsedTime(started));
            LogLeaderboardFailed(ex, ScoresMetrics.LeaderboardKindTop);
            throw;
        }
    }

    private async Task<LevelLeaderboardDto> BuildLeaderboardAsync(GetLevelTopScoresQuery query, CancellationToken cancellationToken)
    {
        var ranked = await repository.GetRankedAsync(query.Level, 0, TopCount, cancellationToken);
        if (ranked.Count == 0)
        {
            return new LevelLeaderboardDto(query.Level, []);
        }

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
