using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;
using HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;
using HexMaster.LeakOut.DailyPipes.Generation;
using HexMaster.LeakOut.DailyPipes.Observability;
using HexMaster.LeakOut.Players.Abstractions.Services;
using Microsoft.Extensions.DependencyInjection;
using Moq;

namespace HexMaster.LeakOut.DailyPipes.Tests;

public sealed class DailyPipesModuleRegistrationTests
{
    [Fact]
    public void AddDailyPipesModule_ResolvesItsServices()
    {
        var services = new ServiceCollection();
        services.AddScoped(_ => Mock.Of<IDailyPipeRepository>());
        services.AddScoped(_ => Mock.Of<IPlayerProfileService>());
        services.AddDailyPipesModule();
        using var provider = services.BuildServiceProvider(new ServiceProviderOptions { ValidateScopes = true, ValidateOnBuild = true });
        using var scope = provider.CreateScope();

        Assert.IsType<SystemClock>(scope.ServiceProvider.GetRequiredService<IClock>());
        Assert.NotNull(scope.ServiceProvider.GetRequiredService<DailyPipesMetrics>());
        Assert.IsType<DailyLevelGenerator>(scope.ServiceProvider.GetRequiredService<IDailyLevelGenerator>());
        Assert.IsType<GetTodaysDailyPipeQueryHandler>(scope.ServiceProvider.GetRequiredService<IQueryHandler<GetTodaysDailyPipeQuery, DailyPipeDto?>>());
        Assert.IsType<SubmitDailyScoreCommandHandler>(scope.ServiceProvider.GetRequiredService<ICommandHandler<SubmitDailyScoreCommand, SubmitDailyScoreOutcome>>());
        Assert.IsType<GetDailyTopScoresQueryHandler>(scope.ServiceProvider.GetRequiredService<IQueryHandler<GetDailyTopScoresQuery, DailyTopScoresDto?>>());
        Assert.IsType<GenerateDailyPipeCommandHandler>(
            scope.ServiceProvider.GetRequiredService<ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>>());
    }

    [Fact]
    public void AddDailyPipesModule_KeepsAnAlreadyRegisteredClock()
    {
        var clock = Mock.Of<IClock>();
        var services = new ServiceCollection().AddSingleton(clock);

        services.AddDailyPipesModule();

        using var provider = services.BuildServiceProvider();
        Assert.Same(clock, provider.GetRequiredService<IClock>());
    }
}
