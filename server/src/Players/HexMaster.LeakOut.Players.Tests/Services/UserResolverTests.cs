using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Factories;
using Microsoft.Extensions.Caching.Memory;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.Services;

public sealed class UserResolverTests : IDisposable
{
    private const string SubjectId = "g1234567890";

    private readonly Mock<IPlayerRepository> _repository = new();
    private readonly MemoryCache _cache = new(new MemoryCacheOptions());

    [Fact]
    public async Task ResolveAsync_RegisteredPlayer_ReturnsInternalId()
    {
        var player = new PlayerFaker().Generate();
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ReturnsAsync(player);

        var playerId = await CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Equal(player.Id, playerId);
    }

    [Fact]
    public async Task ResolveAsync_UnknownPlayer_ReturnsNull()
    {
        var playerId = await CreateResolver().ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Null(playerId);
    }

    [Fact]
    public async Task ResolveAsync_SecondCall_IsServedFromCache()
    {
        var player = new PlayerFaker().Generate();
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ReturnsAsync(player);
        var resolver = CreateResolver();

        await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);
        var playerId = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Equal(player.Id, playerId);
        _repository.Verify(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task ResolveAsync_AfterMiss_FindsNewlyRegisteredPlayer()
    {
        var player = new PlayerFaker().Generate();
        _repository.SetupSequence(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Player?)null)
            .ReturnsAsync(player);
        var resolver = CreateResolver();

        var first = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);
        var second = await resolver.ResolveAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.Null(first);
        Assert.Equal(player.Id, second);
    }

    [Theory]
    [InlineData("")]
    [InlineData(" ")]
    public async Task ResolveAsync_BlankSubjectId_ThrowsArgumentException(string subjectId)
    {
        await Assert.ThrowsAsync<ArgumentException>(
            () => CreateResolver().ResolveAsync(subjectId, TestContext.Current.CancellationToken));
    }

    public void Dispose() => _cache.Dispose();

    private UserResolver CreateResolver() => new(_repository.Object, _cache);
}
