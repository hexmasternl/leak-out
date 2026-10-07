using HexMaster.LeakOut.Players.Services;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.Services;

public sealed class PlayerProfileServiceTests
{
    private readonly Mock<IPlayerRepository> _repository = new();

    [Fact]
    public async Task GetDisplayNamesAsync_KnownPlayers_ReturnsTheirNames()
    {
        var ann = Guid.CreateVersion7();
        var bob = Guid.CreateVersion7();
        _repository
            .Setup(r => r.GetDisplayNamesAsync(It.Is<IReadOnlyCollection<Guid>>(ids => ids.Count == 2), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Dictionary<Guid, string> { [ann] = "Ann", [bob] = "Bob" });

        var names = await CreateService().GetDisplayNamesAsync([ann, bob], TestContext.Current.CancellationToken);

        Assert.Equal("Ann", names[ann]);
        Assert.Equal("Bob", names[bob]);
    }

    [Fact]
    public async Task GetDisplayNamesAsync_UnknownPlayer_IsAbsent()
    {
        var ann = Guid.CreateVersion7();
        var unknown = Guid.CreateVersion7();
        _repository
            .Setup(r => r.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Dictionary<Guid, string> { [ann] = "Ann" });

        var names = await CreateService().GetDisplayNamesAsync([ann, unknown], TestContext.Current.CancellationToken);

        Assert.Equal("Ann", Assert.Single(names).Value);
        Assert.False(names.ContainsKey(unknown));
    }

    [Fact]
    public async Task GetDisplayNamesAsync_DuplicateIds_QueriesEachIdOnce()
    {
        var ann = Guid.CreateVersion7();
        _repository
            .Setup(r => r.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Dictionary<Guid, string> { [ann] = "Ann" });

        await CreateService().GetDisplayNamesAsync([ann, ann], TestContext.Current.CancellationToken);

        _repository.Verify(
            r => r.GetDisplayNamesAsync(It.Is<IReadOnlyCollection<Guid>>(ids => ids.SequenceEqual(new[] { ann })), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task GetDisplayNamesAsync_EmptyIdList_ReturnsEmptyWithoutQuerying()
    {
        var names = await CreateService().GetDisplayNamesAsync([], TestContext.Current.CancellationToken);

        Assert.Empty(names);
        _repository.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task GetDisplayNamesAsync_NullIds_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateService().GetDisplayNamesAsync(null!, TestContext.Current.CancellationToken));
    }

    private PlayerProfileService CreateService() => new(_repository.Object);
}
