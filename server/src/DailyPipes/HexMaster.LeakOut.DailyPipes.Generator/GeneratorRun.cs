using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace HexMaster.LeakOut.DailyPipes.Generator;

/// <summary>
/// One run of the generator job: publishes today's daily pipe and turns the outcome into the process
/// exit code, so a failed run fails the Container Apps job execution and the job retries it.
/// </summary>
internal static partial class GeneratorRun
{
    public const int Success = 0;
    public const int Failure = 1;

    public static async Task<int> RunAsync(IServiceProvider services, ILogger logger, CancellationToken cancellationToken)
    {
        try
        {
            await using var scope = services.CreateAsyncScope();
            var handler = scope.ServiceProvider.GetRequiredService<ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>>();
            var result = await handler.Handle(new GenerateDailyPipeCommand(), cancellationToken);
            LogDone(logger, result.Date, result.Published, result.Attempts);
            return Success;
        }
        catch (Exception ex)
        {
            LogFailed(logger, ex);
            return Failure;
        }
    }

    [LoggerMessage(Level = LogLevel.Information, Message = "Daily pipe run for {Date} finished: published {Published} after {Attempts} attempts")]
    private static partial void LogDone(ILogger logger, DateOnly date, bool published, int attempts);

    [LoggerMessage(Level = LogLevel.Critical, Message = "Daily pipe run failed")]
    private static partial void LogFailed(ILogger logger, Exception exception);
}
