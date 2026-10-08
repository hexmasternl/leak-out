using HexMaster.LeakOut.Players.Data.PostgreSQL;
using HexMaster.LeakOut.Players.Tests.Factories;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.Extensions.DependencyInjection;

namespace HexMaster.LeakOut.Players.Tests.Data;

public sealed class PlayersMigrationsTests : IAsyncLifetime
{
    private readonly SqliteConnection _connection = new("DataSource=:memory:");

    public async ValueTask InitializeAsync() => await _connection.OpenAsync();

    public async ValueTask DisposeAsync() => await _connection.DisposeAsync();

    [Fact]
    public void Model_HasNoChangesMissingFromMigrations()
    {
        using var dbContext = new PlayersDbContextDesignTimeFactory().CreateDbContext([]);

        Assert.False(
            dbContext.Database.HasPendingModelChanges(),
            "The EF model changed without a migration; run 'dotnet ef migrations add'.");
        Assert.Contains(dbContext.Database.GetMigrations(), m => m.EndsWith("_InitialCreate", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Migrator_AppliesMigrationsThatTheRepositoryCanUse()
    {
        var services = new ServiceCollection()
            .AddDbContext<PlayersDbContext>(options => options
                .UseSqlite(_connection)
                // The migrations target PostgreSQL; SQLite sees different provider annotations.
                .ConfigureWarnings(w => w.Ignore(RelationalEventId.PendingModelChangesWarning)))
            .BuildServiceProvider();

        await new PlayersDatabaseMigrator(services.GetRequiredService<IServiceScopeFactory>())
            .StartAsync(TestContext.Current.CancellationToken);

        await using var scope = services.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<PlayersDbContext>();
        var repository = new PlayerRepository(dbContext);
        var player = new PlayerFaker("Ann").Generate();
        await repository.AddAsync("g1", player, TestContext.Current.CancellationToken);

        var found = await repository.FindBySubjectIdAsync("g1", TestContext.Current.CancellationToken);
        Assert.Equal(player.Id, found?.Id);
        Assert.Empty(await dbContext.Database.GetPendingMigrationsAsync(TestContext.Current.CancellationToken));

        await dbContext.GetService<IMigrator>().MigrateAsync(Migration.InitialDatabase, cancellationToken: TestContext.Current.CancellationToken);
        Assert.NotEmpty(await dbContext.Database.GetPendingMigrationsAsync(TestContext.Current.CancellationToken));
        await new PlayersDatabaseMigrator(services.GetRequiredService<IServiceScopeFactory>())
            .StopAsync(TestContext.Current.CancellationToken);
    }
}
