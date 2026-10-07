using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace HexMaster.LeakOut.Scores.Data.PostgreSQL;

/// <summary>
/// Lets <c>dotnet ef</c> create the context without starting the API, which needs the Aspire
/// connection string. Only used to add and list migrations; no connection is opened.
/// </summary>
public sealed class ScoresDbContextDesignTimeFactory : IDesignTimeDbContextFactory<ScoresDbContext>
{
    public ScoresDbContext CreateDbContext(string[] args) =>
        new(new DbContextOptionsBuilder<ScoresDbContext>()
            .UseNpgsql("Host=localhost;Database=scoresdb")
            .Options);
}
