using HexMaster.LeakOut.Scores.Data.PostgreSQL;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Testcontainers.PostgreSql;

namespace HexMaster.LeakOut.Scores.Tests.Helpers;

/// <summary>
/// Runs a PostgreSQL container (Docker is required) and migrates it with
/// <see cref="ScoresDatabaseMigrator"/>, shared by all tests in <see cref="PostgreSqlCollection"/>.
/// Tests isolate their data by using their own player ids.
/// </summary>
public sealed class PostgreSqlFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _container = new PostgreSqlBuilder("postgres:17-alpine")
        .WithDatabase("scoresdb")
        .Build();

    public string ConnectionString => _container.GetConnectionString();

    public async ValueTask InitializeAsync()
    {
        await _container.StartAsync();

        await using var services = new ServiceCollection()
            .AddDbContext<ScoresDbContext>(options => options.UseNpgsql(ConnectionString))
            .BuildServiceProvider();
        await new ScoresDatabaseMigrator(services.GetRequiredService<IServiceScopeFactory>()).StartAsync(CancellationToken.None);
    }

    public async ValueTask DisposeAsync() => await _container.DisposeAsync();

    public ScoresDbContext CreateDbContext() =>
        new(new DbContextOptionsBuilder<ScoresDbContext>().UseNpgsql(ConnectionString).Options);
}

[CollectionDefinition(Name)]
public sealed class PostgreSqlCollection : ICollectionFixture<PostgreSqlFixture>
{
    public const string Name = "PostgreSQL";
}
