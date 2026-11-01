using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

public static class DailyPipesDataRegistration
{
    public const string ConnectionName = "dailypipesdb";

    /// <summary>
    /// Registers the <see cref="DailyPipesDbContext"/> against the Aspire <c>dailypipesdb</c>
    /// connection (with health checks and telemetry) and the <see cref="IDailyPipeRepository"/> implementation.
    /// </summary>
    public static TBuilder AddDailyPipesPostgreSqlData<TBuilder>(this TBuilder builder)
        where TBuilder : IHostApplicationBuilder
    {
        builder.AddNpgsqlDbContext<DailyPipesDbContext>(ConnectionName);
        builder.Services.AddScoped<IDailyPipeRepository, DailyPipeRepository>();
        return builder;
    }
}
