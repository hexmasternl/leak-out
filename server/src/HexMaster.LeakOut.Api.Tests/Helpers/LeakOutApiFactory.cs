using HexMaster.LeakOut.Api.Authentication;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Routing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Extensions.Hosting;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Helpers;

/// <summary>
/// Hosts the real API in Development with mocked Players handlers and resolver, no database
/// access, and one extra protected endpoint, <see cref="ProtectedPath"/>, that echoes the caller's
/// internal player id.
/// </summary>
public sealed class LeakOutApiFactory : WebApplicationFactory<Program>
{
    public const string ProtectedPath = "/test/player";
    public const string SigningKey = "api-tests-signing-key-that-is-long-enough-for-hs256";

    public Mock<ICommandHandler<SignInPlayerCommand, SessionDto>> SignInHandler { get; } = new();

    public Mock<IUserResolver> UserResolver { get; } = new();

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Development");
        builder.UseSetting("ConnectionStrings:playersdb", "Host=localhost;Database=playersdb");
        builder.UseSetting("Aspire:Npgsql:EntityFrameworkCore:PostgreSQL:DisableHealthChecks", "true");
        builder.UseSetting("GooglePlayGames:ClientId", "web-client-id");
        builder.UseSetting("GooglePlayGames:ClientSecret", "web-client-secret");
        builder.UseSetting("Authentication:SigningKey", SigningKey);

        builder.ConfigureTestServices(services =>
        {
            services.RemoveAll<IHostedService>();
            services.RemoveAll<ICommandHandler<SignInPlayerCommand, SessionDto>>();
            services.AddSingleton(SignInHandler.Object);
            services.RemoveAll<IUserResolver>();
            services.AddSingleton(UserResolver.Object);
            services.AddSingleton<IStartupFilter, ProtectedTestEndpointStartupFilter>();
        });
    }

    /// <summary>
    /// Serves <see cref="ProtectedPath"/> from a pipeline branch with the application's own
    /// authentication and authorization services, including the fallback policy.
    /// </summary>
    private sealed class ProtectedTestEndpointStartupFilter : IStartupFilter
    {
        public Action<IApplicationBuilder> Configure(Action<IApplicationBuilder> next) => app =>
        {
            app.MapWhen(
                context => context.Request.Path == ProtectedPath,
                branch =>
                {
                    branch.UseRouting();
                    branch.UseAuthentication();
                    branch.UseAuthorization();
                    branch.UseEndpoints(endpoints =>
                        endpoints.MapGet(ProtectedPath, (HttpContext context) => TypedResults.Ok(context.GetPlayerId()))
                            .RequirePlayer());
                });
            next(app);
        };
    }
}
