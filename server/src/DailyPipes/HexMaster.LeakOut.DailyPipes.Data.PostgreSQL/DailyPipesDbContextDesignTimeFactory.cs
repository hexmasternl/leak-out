using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;

/// <summary>
/// Lets <c>dotnet ef</c> create the context without starting the API, which needs the Aspire
/// connection string. Only used to add and list migrations; no connection is opened.
/// </summary>
public sealed class DailyPipesDbContextDesignTimeFactory : IDesignTimeDbContextFactory<DailyPipesDbContext>
{
    public DailyPipesDbContext CreateDbContext(string[] args) =>
        new(new DbContextOptionsBuilder<DailyPipesDbContext>()
            .UseNpgsql("Host=localhost;Database=dailypipesdb")
            .Options);
}
