using HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Testcontainers.PostgreSql;

namespace HexMaster.LeakOut.DailyPipes.Tests.Helpers;

/// <summary>
/// Runs a PostgreSQL container (Docker is required) and migrates it with
/// <see cref="DailyPipesDatabaseMigrator"/>, shared by all tests in <see cref="PostgreSqlCollection"/>.
/// Tests isolate their data by using their own player ids and dates.
/// </summary>
public sealed class PostgreSqlFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _container = new PostgreSqlBuilder("postgres:17-alpine")
        .WithDatabase("dailypipesdb")
        .Build();

    public string ConnectionString => _container.GetConnectionString();

    public async ValueTask InitializeAsync()
    {
        await _container.StartAsync();

        await using var services = new ServiceCollection()
            .AddDbContext<DailyPipesDbContext>(options => options.UseNpgsql(ConnectionString))
            .BuildServiceProvider();
        await new DailyPipesDatabaseMigrator(services.GetRequiredService<IServiceScopeFactory>()).StartAsync(CancellationToken.None);
    }

    public async ValueTask DisposeAsync() => await _container.DisposeAsync();

    /// <summary>A context with the retrying execution strategy the Aspire registration uses.</summary>
    public DailyPipesDbContext CreateDbContext() =>
        new(new DbContextOptionsBuilder<DailyPipesDbContext>()
            .UseNpgsql(ConnectionString, npgsql => npgsql.EnableRetryOnFailure())
            .Options);
}

[CollectionDefinition(Name)]
public sealed class PostgreSqlCollection : ICollectionFixture<PostgreSqlFixture>
{
    public const string Name = "PostgreSQL";
}
