using HexMaster.LeakOut.Players.Data.PostgreSQL;
using HexMaster.LeakOut.Scores.Data.PostgreSQL;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Extensions.Hosting;

namespace HexMaster.LeakOut.Api.Tests;

public sealed class DatabaseMigrationRegistrationTests
{
    [Theory]
    [InlineData("Development")]
    [InlineData("Production")]
    public void Program_RegistersBothDatabaseMigrators(string environment)
    {
        using var factory = new HostedServiceCapturingFactory(environment);

        _ = factory.Services;

        Assert.Contains(typeof(PlayersDatabaseMigrator), factory.HostedServiceTypes);
        Assert.Contains(typeof(ScoresDatabaseMigrator), factory.HostedServiceTypes);
    }

    /// <summary>
    /// Hosts the real API in the given environment and records which hosted services it
    /// registers, then removes them so no database is touched.
    /// </summary>
    private sealed class HostedServiceCapturingFactory(string environment) : WebApplicationFactory<Program>
    {
        public List<Type?> HostedServiceTypes { get; } = [];

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.UseEnvironment(environment);
            builder.UseSetting("ConnectionStrings:playersdb", "Host=localhost;Database=playersdb");
            builder.UseSetting("ConnectionStrings:scoresdb", "Host=localhost;Database=scoresdb");
            builder.UseSetting("Aspire:Npgsql:EntityFrameworkCore:PostgreSQL:DisableHealthChecks", "true");
            builder.UseSetting("GooglePlayGames:ClientId", "web-client-id");
            builder.UseSetting("GooglePlayGames:ClientSecret", "web-client-secret");
            builder.UseSetting("Authentication:SigningKey", "migration-tests-signing-key-long-enough-for-hs256");

            builder.ConfigureTestServices(services =>
            {
                HostedServiceTypes.AddRange(services
                    .Where(descriptor => descriptor.ServiceType == typeof(IHostedService))
                    .Select(descriptor => descriptor.ImplementationType));
                services.RemoveAll<IHostedService>();
            });
        }
    }
}
