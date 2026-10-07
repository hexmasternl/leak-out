using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

public static class ScoresDataRegistration
{
    public const string ConnectionName = "scoresdb";

    /// <summary>
    /// Registers the <see cref="ScoresDbContext"/> against the Aspire <c>scoresdb</c> connection
    /// (with health checks and telemetry) and the <see cref="ILevelScoreRepository"/> implementation.
    /// </summary>
    public static TBuilder AddScoresPostgreSqlData<TBuilder>(this TBuilder builder)
        where TBuilder : IHostApplicationBuilder
    {
        builder.AddNpgsqlDbContext<ScoresDbContext>(ConnectionName);
        builder.Services.AddScoped<ILevelScoreRepository, LevelScoreRepository>();
        return builder;
    }
}
