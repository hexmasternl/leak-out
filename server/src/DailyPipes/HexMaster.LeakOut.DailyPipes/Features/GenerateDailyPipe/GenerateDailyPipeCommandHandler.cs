using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.DomainModels;
using HexMaster.LeakOut.DailyPipes.Generation;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;

/// <summary>
/// Publishes today's daily pipe: generates candidates until one passes validation and the solver, up
/// to <see cref="MaxAttempts"/>, and publishes it for the UTC date of <see cref="IClock"/>. Older days'
/// scores and pipes older than <see cref="KeptPipeDays"/> days are deleted with the publication. When
/// today already has a pipe, nothing is generated and the pipe is left unchanged.
/// </summary>
public sealed partial class GenerateDailyPipeCommandHandler(
    IDailyPipeRepository repository,
    IDailyLevelGenerator generator,
    IClock clock,
    DailyPipesMetrics metrics,
    ILogger<GenerateDailyPipeCommandHandler> logger) : ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>
{
    public const int MaxAttempts = 200;

    /// <summary>How many days a published pipe is kept for diagnosis; its scores go the next day.</summary>
    public const int KeptPipeDays = 7;

    public async Task<GenerateDailyPipeResult> Handle(GenerateDailyPipeCommand command, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(command);

        var started = Stopwatch.GetTimestamp();
        var now = clock.UtcNow;
        var today = DateOnly.FromDateTime(now.UtcDateTime);
        using var activity = DailyPipesMetrics.ActivitySource.StartActivity(DailyPipesMetrics.GenerateDailyPipeActivityName);
        activity?.SetTag("daily_pipe.date", today.ToString("yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture));
        var attempts = 0;
        try
        {
            if (await repository.GetPipeAsync(today, cancellationToken) is not null)
            {
                return AlreadyPublished(today, attempts, started, activity);
            }

            BuiltDailyPipe? built = null;
            while (built is null && attempts < MaxAttempts)
            {
                attempts++;
                built = DailyPipeBuilder.TryBuild(generator.Generate(today));
            }

            if (built is null)
            {
                throw new DailyPipeGenerationException(
                    $"No daily pipe for {today:yyyy-MM-dd} passed validation and the solver within {MaxAttempts} attempts.");
            }

            var pipe = DailyPipe.Create(today, built.LevelJson, built.StackSeed, built.MaxScore, now);
            if (!await repository.PublishAsync(pipe, today.AddDays(-KeptPipeDays), cancellationToken))
            {
                return AlreadyPublished(today, attempts, started, activity);
            }

            activity?.SetTag("daily_pipe.attempts", attempts);
            activity?.SetTag("daily_pipe.published", true);
            activity?.SetStatus(ActivityStatusCode.Ok);
            metrics.GenerationRun(DailyPipesMetrics.OutcomePublished, attempts, Stopwatch.GetElapsedTime(started));
            LogPublished(today, attempts, built.Level.Grid.Columns, built.Level.Grid.Rows);
            return new GenerateDailyPipeResult(today, Published: true, attempts);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            activity?.SetTag("daily_pipe.attempts", attempts);
            activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
            activity?.AddException(ex);
            metrics.GenerationRun(DailyPipesMetrics.OutcomeFailed, attempts, Stopwatch.GetElapsedTime(started));
            LogGenerationFailed(ex, today, attempts);
            throw;
        }
    }

    private GenerateDailyPipeResult AlreadyPublished(DateOnly today, int attempts, long started, Activity? activity)
    {
        activity?.SetTag("daily_pipe.attempts", attempts);
        activity?.SetTag("daily_pipe.published", false);
        activity?.SetStatus(ActivityStatusCode.Ok);
        metrics.GenerationRun(DailyPipesMetrics.OutcomeAlreadyPublished, attempts, Stopwatch.GetElapsedTime(started));
        LogAlreadyPublished(today);
        return new GenerateDailyPipeResult(today, Published: false, attempts);
    }

    [LoggerMessage(Level = LogLevel.Information, Message = "Published the daily pipe of {Date} after {Attempts} attempts on a {Columns} by {Rows} grid")]
    private partial void LogPublished(DateOnly date, int attempts, int columns, int rows);

    [LoggerMessage(Level = LogLevel.Information, Message = "The daily pipe of {Date} was already published; it is left unchanged")]
    private partial void LogAlreadyPublished(DateOnly date);

    [LoggerMessage(Level = LogLevel.Error, Message = "Generating the daily pipe of {Date} failed after {Attempts} attempts")]
    private partial void LogGenerationFailed(Exception exception, DateOnly date, int attempts);
}
