using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Observability;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace HexMaster.LeakOut.DailyPipes;

public static class DailyPipesModuleRegistration
{
    /// <summary>
    /// Registers the DailyPipes module's handlers and telemetry. The <see cref="IDailyPipeRepository"/>
    /// is registered by the data project, the <c>IPlayerProfileService</c> by the Players module.
    /// </summary>
    public static IServiceCollection AddDailyPipesModule(this IServiceCollection services)
    {
        services.AddLogging();
        services.AddMetrics();
        services.TryAddSingleton<IClock, SystemClock>();
        services.TryAddSingleton<DailyPipesMetrics>();

        return services;
    }
}
