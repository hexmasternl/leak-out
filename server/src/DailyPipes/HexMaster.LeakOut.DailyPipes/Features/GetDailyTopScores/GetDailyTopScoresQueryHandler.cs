using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Observability;
using HexMaster.LeakOut.Players.Abstractions.Services;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;

/// <summary>
/// Returns the <see cref="TopCount"/> best results on the daily pipe of a date, ranked by score and
/// then by the earliest achievement, with the requesting player's own rank; <see langword="null"/>
/// when the date has no published pipe.
/// </summary>
public sealed partial class GetDailyTopScoresQueryHandler(
    IDailyPipeRepository repository,
    IPlayerProfileService playerProfiles,
    DailyPipesMetrics metrics,
    ILogger<GetDailyTopScoresQueryHandler> logger) : IQueryHandler<GetDailyTopScoresQuery, DailyTopScoresDto?>
{
    public const int TopCount = 10;
    public const string UnknownPlayerName = "Unknown player";

    public async Task<DailyTopScoresDto?> Handle(GetDailyTopScoresQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        using var activity = DailyPipesMetrics.ActivitySource.StartActivity(DailyPipesMetrics.GetDailyTopScoresActivityName);
        activity?.SetTag("player.id", query.UserId);
        try
        {
            if (await repository.GetPipeAsync(query.Date, cancellationToken) is null)
            {
                activity?.SetStatus(ActivityStatusCode.Ok);
                metrics.LeaderboardRequest(DailyPipesMetrics.OutcomeNotFound);
                return null;
            }

            var top = await repository.GetTopAsync(query.Date, TopCount, cancellationToken);
            var names = top.Count == 0
                ? new Dictionary<Guid, string>()
                : await playerProfiles.GetDisplayNamesAsync(top.Select(s => s.UserId).ToArray(), cancellationToken);
            var entries = top
                .Select(s => new DailyLeaderboardEntryDto(
                    s.Position,
                    names.TryGetValue(s.UserId, out var name) && !string.IsNullOrWhiteSpace(name) ? name : UnknownPlayerName,
                    s.Score,
                    s.Stars,
                    s.UserId == query.UserId))
                .ToList();

            var best = await repository.GetBestAsync(query.Date, query.UserId, cancellationToken);
            DailyPlayerRankDto? player = null;
            if (best is not null)
            {
                var above = await repository.CountRankedAboveAsync(query.Date, best.Score, best.AchievedAt, query.UserId, cancellationToken);
                player = new DailyPlayerRankDto(above + 1, best.Score, best.Stars);
            }

            activity?.SetTag("leaderboard.entries", entries.Count);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.LeaderboardRequest(DailyPipesMetrics.OutcomeServed);
            return new DailyTopScoresDto(query.Date, entries, player);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.LeaderboardRequest(DailyPipesMetrics.OutcomeError);
            LogFailed(ex);
            throw;
        }
    }

    [LoggerMessage(Level = LogLevel.Error, Message = "Daily top scores request failed unexpectedly")]
    private partial void LogFailed(Exception exception);
}
