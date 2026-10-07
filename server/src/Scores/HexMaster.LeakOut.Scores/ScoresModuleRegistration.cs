using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.SubmitScore;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace HexMaster.LeakOut.Scores;

public static class ScoresModuleRegistration
{
    /// <summary>
    /// Registers the Scores module's handlers and telemetry. The <see cref="ILevelScoreRepository"/>
    /// is registered by the data project.
    /// </summary>
    public static IServiceCollection AddScoresModule(this IServiceCollection services)
    {
        services.AddMetrics();
        services.TryAddSingleton<IClock, SystemClock>();
        services.TryAddSingleton<ScoresMetrics>();

        services.AddScoped<ICommandHandler<SubmitScoreCommand, SubmitScoreResult>, SubmitScoreCommandHandler>();

        return services;
    }
}
