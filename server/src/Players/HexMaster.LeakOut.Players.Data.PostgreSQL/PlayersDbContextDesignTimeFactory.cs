using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace HexMaster.LeakOut.Players.Data.PostgreSQL;

/// <summary>
/// Lets <c>dotnet ef</c> create the context without starting the API, which needs the Aspire
/// connection string. Only used to add and list migrations; no connection is opened.
/// </summary>
public sealed class PlayersDbContextDesignTimeFactory : IDesignTimeDbContextFactory<PlayersDbContext>
{
    public PlayersDbContext CreateDbContext(string[] args) =>
        new(new DbContextOptionsBuilder<PlayersDbContext>()
            .UseNpgsql("Host=localhost;Database=playersdb")
            .Options);
}
