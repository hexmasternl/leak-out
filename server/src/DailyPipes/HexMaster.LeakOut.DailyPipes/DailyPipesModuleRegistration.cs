using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;
using HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;
using HexMaster.LeakOut.DailyPipes.Generation;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace HexMaster.LeakOut.DailyPipes;

public static class DailyPipesModuleRegistration
{
    /// <summary>
    /// Registers the DailyPipes module's handlers, the level generator and telemetry. The
    /// <see cref="IDailyPipeRepository"/> is registered by the data project, the
    /// <c>IPlayerProfileService</c> by the Players module.
    /// </summary>
    public static IServiceCollection AddDailyPipesModule(this IServiceCollection services)
    {
        services.AddLogging();
        services.AddMetrics();
        services.TryAddSingleton<IClock, SystemClock>();
        services.TryAddSingleton<DailyPipesMetrics>();
        services.TryAddSingleton<IDailyLevelGenerator, DailyLevelGenerator>();

        services.AddScoped<ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>, GenerateDailyPipeCommandHandler>();
        services.AddScoped<IQueryHandler<GetTodaysDailyPipeQuery, DailyPipeDto?>, GetTodaysDailyPipeQueryHandler>();
        services.AddScoped<ICommandHandler<SubmitDailyScoreCommand, SubmitDailyScoreOutcome>, SubmitDailyScoreCommandHandler>();
        services.AddScoped<IQueryHandler<GetDailyTopScoresQuery, DailyTopScoresDto?>, GetDailyTopScoresQueryHandler>();

        return services;
    }
}
