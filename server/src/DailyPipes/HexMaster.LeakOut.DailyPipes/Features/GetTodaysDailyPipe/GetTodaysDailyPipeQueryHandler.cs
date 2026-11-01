using System.Diagnostics;
using System.Text.Json;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;

/// <summary>
/// Returns the daily pipe of the server's UTC date with the player's streak and best result on it, or
/// <see langword="null"/> when today's pipe is not published yet. An older pipe is never returned.
/// </summary>
public sealed partial class GetTodaysDailyPipeQueryHandler(
    IDailyPipeRepository repository,
    IClock clock,
    DailyPipesMetrics metrics,
    ILogger<GetTodaysDailyPipeQueryHandler> logger) : IQueryHandler<GetTodaysDailyPipeQuery, DailyPipeDto?>
{
    public async Task<DailyPipeDto?> Handle(GetTodaysDailyPipeQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var today = clock.UtcNow.UtcDate();
        using var activity = DailyPipesMetrics.ActivitySource.StartActivity(DailyPipesMetrics.GetTodaysDailyPipeActivityName);
        activity?.SetTag("player.id", query.UserId);
        try
        {
            var pipe = await repository.GetPipeAsync(today, cancellationToken);
            activity?.SetTag("daily_pipe.found", pipe is not null);
            if (pipe is null)
            {
                activity?.SetStatus(ActivityStatusCode.Ok);
                metrics.Download(DailyPipesMetrics.OutcomeNotFound);
                return null;
            }

            var streak = await repository.GetStreakAsync(query.UserId, cancellationToken);
            var best = await repository.GetBestAsync(today, query.UserId, cancellationToken);
            using var level = JsonDocument.Parse(pipe.LevelJson);

            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.Download(DailyPipesMetrics.OutcomeServed);
            return new DailyPipeDto(
                pipe.Date,
                level.RootElement.Clone(),
                pipe.StackSeed,
                streak.ToDto(today),
                best is null ? null : new DailyBestScoreDto(best.Score, best.Stars));
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.Download(DailyPipesMetrics.OutcomeError);
            LogFailed(ex);
            throw;
        }
    }

    [LoggerMessage(Level = LogLevel.Error, Message = "Reading today's daily pipe failed unexpectedly")]
    private partial void LogFailed(Exception exception);
}
