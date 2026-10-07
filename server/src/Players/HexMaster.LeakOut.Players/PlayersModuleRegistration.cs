using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Players.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace HexMaster.LeakOut.Players;

public static class PlayersModuleRegistration
{
    /// <summary>
    /// Registers the Players module: its handlers, the Google Play Games client, the access token
    /// issuer and the user resolver. The <see cref="IPlayerRepository"/> is registered by the data project.
    /// </summary>
    public static IServiceCollection AddPlayersModule(this IServiceCollection services)
    {
        services.AddOptions<GooglePlayGamesOptions>()
            .BindConfiguration(GooglePlayGamesOptions.SectionName)
            .ValidateDataAnnotations()
            .ValidateOnStart();
        services.AddOptions<AccessTokenOptions>()
            .BindConfiguration(AccessTokenOptions.SectionName)
            .ValidateDataAnnotations()
            .ValidateOnStart();

        services.AddMemoryCache();
        services.AddMetrics();
        services.TryAddSingleton<IClock, SystemClock>();
        services.TryAddSingleton<PlayersMetrics>();

        services.AddHttpClient<IGooglePlayGamesClient, GooglePlayGamesClient>();
        services.AddSingleton<IAccessTokenIssuer, JwtAccessTokenIssuer>();
        services.AddScoped<IUserResolver, UserResolver>();

        services.AddScoped<ICommandHandler<SignInPlayerCommand, SessionDto>, SignInPlayerCommandHandler>();

        return services;
    }
}
