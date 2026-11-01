using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;

/// <summary>
/// Takes a result on the current daily pipe: only the pipe of the server's UTC date accepts
/// submissions, a score above the pipe's maximum is refused, a higher score replaces the player's
/// best, and a result with at least one star is a win that counts in the streak.
/// </summary>
public sealed partial class SubmitDailyScoreCommandHandler(
    IDailyPipeRepository repository,
    IClock clock,
    DailyPipesMetrics metrics,
    ILogger<SubmitDailyScoreCommandHandler> logger) : ICommandHandler<SubmitDailyScoreCommand, SubmitDailyScoreOutcome>
{
    public async Task<SubmitDailyScoreOutcome> Handle(SubmitDailyScoreCommand command, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(command);

        var started = Stopwatch.GetTimestamp();
        var now = clock.UtcNow;
        var today = now.UtcDate();
        using var activity = DailyPipesMetrics.ActivitySource.StartActivity(DailyPipesMetrics.SubmitDailyScoreActivityName);
        activity?.SetTag("player.id", command.UserId);
        activity?.SetTag("score.value", command.Score);
        activity?.SetTag("score.stars", command.Stars);
        try
        {
            var pipe = command.Date == today ? await repository.GetPipeAsync(today, cancellationToken) : null;
            if (pipe is null)
            {
                return Done(new SubmitDailyScoreOutcome.Ended(), DailyPipesMetrics.OutcomeEnded, started, activity);
            }

            if (command.Score > pipe.MaxScore)
            {
                return Done(new SubmitDailyScoreOutcome.ScoreTooHigh(pipe.MaxScore), DailyPipesMetrics.OutcomeRejected, started, activity);
            }

            var submission = await repository.SubmitScoreAsync(today, command.UserId, command.Score, command.Stars, now, cancellationToken);
            var streak = command.Stars >= 1
                ? await repository.RecordWinAsync(command.UserId, today, cancellationToken)
                : await repository.GetStreakAsync(command.UserId, cancellationToken);
            var rank = await repository.CountRankedAboveAsync(
                today, submission.BestScore, submission.AchievedAt, command.UserId, cancellationToken) + 1;

            activity?.SetTag("score.new_best", submission.IsNewBest);
            if (submission.IsNewBest)
            {
                LogNewBest(command.UserId, submission.BestScore, today);
            }

            var result = new DailyScoreResultDto(
                today,
                new DailyBestScoreDto(submission.BestScore, submission.BestStars),
                submission.IsNewBest,
                rank,
                streak.ToDto(today));
            var outcome = submission.IsNewBest ? DailyPipesMetrics.OutcomeNewBest : DailyPipesMetrics.OutcomeNotImproved;
            return Done(new SubmitDailyScoreOutcome.Accepted(result), outcome, started, activity);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.Submission(DailyPipesMetrics.OutcomeError, Stopwatch.GetElapsedTime(started));
            LogFailed(ex);
            throw;
        }
    }

    private SubmitDailyScoreOutcome Done(SubmitDailyScoreOutcome outcome, string metricOutcome, long started, Activity? activity)
    {
        activity?.SetTag("daily_pipe.outcome", metricOutcome);
        activity?.SetStatus(ActivityStatusCode.Ok);
        metrics.Submission(metricOutcome, Stopwatch.GetElapsedTime(started));
        return outcome;
    }

    [LoggerMessage(Level = LogLevel.Information, Message = "Player {PlayerId} set a new best score {Score} on the daily pipe of {Date}")]
    private partial void LogNewBest(Guid playerId, int score, DateOnly date);

    [LoggerMessage(Level = LogLevel.Error, Message = "Daily pipe score submission failed unexpectedly")]
    private partial void LogFailed(Exception exception);
}
