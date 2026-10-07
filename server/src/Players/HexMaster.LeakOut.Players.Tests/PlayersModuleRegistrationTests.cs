using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Moq;

namespace HexMaster.LeakOut.Players.Tests;

public sealed class PlayersModuleRegistrationTests
{
    private static readonly Dictionary<string, string?> ValidSettings = new()
    {
        ["GooglePlayGames:ClientId"] = "web-client-id",
        ["GooglePlayGames:ClientSecret"] = "web-client-secret",
        ["Authentication:SigningKey"] = "a-test-signing-key-that-is-long-enough-for-hs256",
    };

    [Fact]
    public void AddPlayersModule_ResolvesSignInHandlerAndUserResolver()
    {
        using var provider = BuildProvider(ValidSettings);
        using var scope = provider.CreateScope();

        Assert.IsType<SignInPlayerCommandHandler>(
            scope.ServiceProvider.GetRequiredService<ICommandHandler<SignInPlayerCommand, SessionDto>>());
        Assert.IsType<UserResolver>(scope.ServiceProvider.GetRequiredService<IUserResolver>());
        Assert.IsType<PlayerProfileService>(scope.ServiceProvider.GetRequiredService<IPlayerProfileService>());
        Assert.IsType<GooglePlayGamesClient>(scope.ServiceProvider.GetRequiredService<IGooglePlayGamesClient>());
        Assert.IsType<SystemClock>(scope.ServiceProvider.GetRequiredService<IClock>());
    }

    [Fact]
    public void AddPlayersModule_BindsOptionsFromConfiguration()
    {
        using var provider = BuildProvider(ValidSettings);

        var google = provider.GetRequiredService<IOptions<GooglePlayGamesOptions>>().Value;
        var tokens = provider.GetRequiredService<IOptions<AccessTokenOptions>>().Value;

        Assert.Equal("web-client-id", google.ClientId);
        Assert.Equal("web-client-secret", google.ClientSecret);
        Assert.Equal("leak-out-server", tokens.Issuer);
        Assert.Equal(TimeSpan.FromHours(1), tokens.Lifetime);
    }

    [Theory]
    [InlineData("GooglePlayGames:ClientId")]
    [InlineData("GooglePlayGames:ClientSecret")]
    [InlineData("Authentication:SigningKey")]
    public void AddPlayersModule_MissingSetting_FailsOptionsValidation(string missingKey)
    {
        var settings = new Dictionary<string, string?>(ValidSettings) { [missingKey] = null };
        using var provider = BuildProvider(settings);

        var exception = Assert.Throws<OptionsValidationException>(() =>
        {
            _ = provider.GetRequiredService<IOptions<GooglePlayGamesOptions>>().Value;
            _ = provider.GetRequiredService<IOptions<AccessTokenOptions>>().Value;
        });
        Assert.Contains(missingKey.Split(':')[1], exception.Message);
    }

    private static ServiceProvider BuildProvider(Dictionary<string, string?> settings)
    {
        var services = new ServiceCollection();
        services.AddSingleton<IConfiguration>(new ConfigurationBuilder().AddInMemoryCollection(settings).Build());
        services.AddLogging();
        services.AddScoped(_ => Mock.Of<IPlayerRepository>());
        services.AddPlayersModule();
        return services.BuildServiceProvider(new ServiceProviderOptions { ValidateScopes = true, ValidateOnBuild = true });
    }
}
