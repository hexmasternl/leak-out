using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.Scores.Features.SubmitScore;

/// <summary>
/// Keeps the submitted score when it is the player's first or a strictly higher score on the level,
/// and returns the stored best score either way.
/// </summary>
public sealed partial class SubmitScoreCommandHandler(
    ILevelScoreRepository repository,
    IClock clock,
    ScoresMetrics metrics,
    ILogger<SubmitScoreCommandHandler> logger) : ICommandHandler<SubmitScoreCommand, SubmitScoreResult>
{
    public async Task<SubmitScoreResult> Handle(SubmitScoreCommand command, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(command);

        var started = Stopwatch.GetTimestamp();
        using var activity = ScoresMetrics.ActivitySource.StartActivity(ScoresMetrics.SubmitScoreActivityName);
        activity?.SetTag("player.id", command.UserId);
        activity?.SetTag("score.level", command.Level);
        activity?.SetTag("score.value", command.Score);
        try
        {
            var submission = await repository.SubmitAsync(
                command.UserId, command.Level, command.Score, clock.UtcNow, cancellationToken);

            activity?.SetTag("score.new_best", submission.IsNewBest);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.ScoreSubmitted(submission.IsNewBest, Stopwatch.GetElapsedTime(started));
            if (submission.IsNewBest)
            {
                LogNewBestScore(command.UserId, submission.BestScore, command.Level);
            }

            return new SubmitScoreResult(command.Level, submission.BestScore, submission.IsNewBest);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.ScoreSubmissionFailed(Stopwatch.GetElapsedTime(started));
            LogSubmissionFailed(ex);
            throw;
        }
    }

    [LoggerMessage(Level = LogLevel.Information, Message = "Player {PlayerId} set a new best score {Score} on level {Level}")]
    private partial void LogNewBestScore(Guid playerId, int score, int level);

    [LoggerMessage(Level = LogLevel.Error, Message = "Score submission failed unexpectedly")]
    private partial void LogSubmissionFailed(Exception exception);
}
