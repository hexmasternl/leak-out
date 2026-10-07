using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.Players.Data.PostgreSQL;

public static class PlayersDataRegistration
{
    public const string ConnectionName = "playersdb";

    /// <summary>
    /// Registers the <see cref="PlayersDbContext"/> against the Aspire <c>playersdb</c> connection
    /// (with health checks and telemetry) and the <see cref="IPlayerRepository"/> implementation.
    /// </summary>
    public static TBuilder AddPlayersPostgreSqlData<TBuilder>(this TBuilder builder)
        where TBuilder : IHostApplicationBuilder
    {
        builder.AddNpgsqlDbContext<PlayersDbContext>(ConnectionName);
        builder.Services.AddScoped<IPlayerRepository, PlayerRepository>();
        return builder;
    }
}
