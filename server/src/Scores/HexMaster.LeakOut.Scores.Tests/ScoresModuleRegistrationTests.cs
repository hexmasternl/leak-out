using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;
using HexMaster.LeakOut.Scores.Features.SubmitScore;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.DependencyInjection;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests;

public sealed class ScoresModuleRegistrationTests
{
    [Fact]
    public void AddScoresModule_ResolvesHandlers()
    {
        var services = new ServiceCollection();
        services.AddScoped(_ => Mock.Of<ILevelScoreRepository>());
        services.AddScoped(_ => Mock.Of<IPlayerProfileService>());
        services.AddScoresModule();
        using var provider = services.BuildServiceProvider(new ServiceProviderOptions { ValidateScopes = true, ValidateOnBuild = true });
        using var scope = provider.CreateScope();

        Assert.IsType<SubmitScoreCommandHandler>(
            scope.ServiceProvider.GetRequiredService<ICommandHandler<SubmitScoreCommand, SubmitScoreResult>>());
        Assert.IsType<GetLevelTopScoresQueryHandler>(
            scope.ServiceProvider.GetRequiredService<IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto>>());
        Assert.IsType<GetLevelScoresAroundPlayerQueryHandler>(
            scope.ServiceProvider.GetRequiredService<IQueryHandler<GetLevelScoresAroundPlayerQuery, LevelLeaderboardDto>>());
        Assert.IsType<GetPublicLevelTopScoresQueryHandler>(
            scope.ServiceProvider.GetRequiredService<IQueryHandler<GetPublicLevelTopScoresQuery, PublicLevelLeaderboardDto>>());
        Assert.NotNull(scope.ServiceProvider.GetRequiredService<IMemoryCache>());
        Assert.IsType<SystemClock>(scope.ServiceProvider.GetRequiredService<IClock>());
        Assert.NotNull(scope.ServiceProvider.GetRequiredService<ScoresMetrics>());
    }

    [Fact]
    public void AddScoresModule_KeepsAnAlreadyRegisteredClock()
    {
        var clock = Mock.Of<IClock>();
        var services = new ServiceCollection().AddSingleton(clock);

        services.AddScoresModule();

        using var provider = services.BuildServiceProvider();
        Assert.Same(clock, provider.GetRequiredService<IClock>());
    }
}
