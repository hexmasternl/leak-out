using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

/// <summary>
/// Applies pending migrations of the daily pipes database when the host starts. Registered in
/// every environment; hosted services start before the server listens, so the API serves no
/// request against an out-of-date schema, and a failed migration stops the host.
/// </summary>
public sealed class DailyPipesDatabaseMigrator(IServiceScopeFactory scopeFactory) : IHostedService
{
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        await using var scope = scopeFactory.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<DailyPipesDbContext>();
        await dbContext.Database.MigrateAsync(cancellationToken);
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
