using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.DailyPipes.Data.PostgreSQL;
using HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;
using HexMaster.LeakOut.DailyPipes.Generator;
using HexMaster.LeakOut.DailyPipes.Tests.Helpers;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generator;

[Collection(PostgreSqlCollection.Name)]
public sealed class GeneratorRunTests(PostgreSqlFixture database)
{
    private readonly FakeLogger _logger = new();

    private static CancellationToken Ct => TestContext.Current.CancellationToken;

    [Fact]
    public async Task RunAsync_Published_ExitsWithSuccess()
    {
        var handler = new Mock<ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>>();
        handler.Setup(h => h.Handle(It.IsAny<GenerateDailyPipeCommand>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GenerateDailyPipeResult(new DateOnly(2026, 11, 2), Published: true, Attempts: 3));
        await using var services = new ServiceCollection().AddSingleton(handler.Object).BuildServiceProvider();

        var exitCode = await GeneratorRun.RunAsync(services, _logger, Ct);

        Assert.Equal(GeneratorRun.Success, exitCode);
        Assert.Contains("finished: published True after 3 attempts", _logger.LatestRecord.Message, StringComparison.Ordinal);
    }

    [Fact]
    public async Task RunAsync_Failed_ExitsWithFailureAndLogsIt()
    {
        var handler = new Mock<ICommandHandler<GenerateDailyPipeCommand, GenerateDailyPipeResult>>();
        handler.Setup(h => h.Handle(It.IsAny<GenerateDailyPipeCommand>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new DailyPipeGenerationException("no candidate"));
        await using var services = new ServiceCollection().AddSingleton(handler.Object).BuildServiceProvider();

        var exitCode = await GeneratorRun.RunAsync(services, _logger, Ct);

        Assert.Equal(GeneratorRun.Failure, exitCode);
        Assert.Equal(LogLevel.Critical, _logger.LatestRecord.Level);
        Assert.IsType<DailyPipeGenerationException>(_logger.LatestRecord.Exception);
    }

    [Fact]
    public async Task RunAsync_AgainstTheDatabase_PublishesOnceAndLeavesItOnARerun()
    {
        var today = new DateOnly(2099, 6, 1);
        var clock = new Mock<IClock>();
        clock.Setup(c => c.UtcNow).Returns(new DateTimeOffset(2099, 6, 1, 0, 0, 2, TimeSpan.Zero));
        await using var services = new ServiceCollection()
            .AddSingleton(clock.Object)
            .AddDailyPipesModule()
            .AddDbContext<DailyPipesDbContext>(options => options.UseNpgsql(database.ConnectionString))
            .AddScoped<IDailyPipeRepository, DailyPipeRepository>()
            .BuildServiceProvider();

        Assert.Equal(GeneratorRun.Success, await GeneratorRun.RunAsync(services, _logger, Ct));
        await using var db = database.CreateDbContext();
        var first = await new DailyPipeRepository(db).GetPipeAsync(today, Ct);
        Assert.Equal(GeneratorRun.Success, await GeneratorRun.RunAsync(services, _logger, Ct));
        var second = await new DailyPipeRepository(db).GetPipeAsync(today, Ct);

        Assert.NotNull(first);
        Assert.Equal(first.LevelJson, second!.LevelJson);
        Assert.Equal(first.StackSeed, second.StackSeed);
        Assert.Contains("published False", _logger.LatestRecord.Message, StringComparison.Ordinal);
    }
}
