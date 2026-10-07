using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

/// <summary>
/// Applies pending migrations of the scores database when the host starts. Registered in
/// Development only; production migrations are a deployment step.
/// </summary>
public sealed class ScoresDatabaseMigrator(IServiceScopeFactory scopeFactory) : IHostedService
{
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        await using var scope = scopeFactory.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<ScoresDbContext>();
        await dbContext.Database.MigrateAsync(cancellationToken);
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
