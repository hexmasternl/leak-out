using System.Diagnostics;
using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Helpers;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.Services;

[Collection(TelemetryCollection.Name)]
public sealed class PlayerProfileServiceTests : IDisposable
{
    private readonly Mock<IPlayerRepository> _repository = new();
    private readonly TelemetryCollector _telemetry = new();

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

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(PlayersMetrics.GetDisplayNamesActivityName, activity.OperationName);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(2, activity.GetTagItem("players.requested"));
        Assert.Equal(2, activity.GetTagItem("players.found"));
        Assert.DoesNotContain(activity.TagObjects, t => t.Value is "Ann" or "Bob");
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

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(2, activity.GetTagItem("players.requested"));
        Assert.Equal(1, activity.GetTagItem("players.found"));
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
        Assert.Equal(1, Assert.Single(_telemetry.Activities).GetTagItem("players.requested"));
    }

    [Fact]
    public async Task GetDisplayNamesAsync_EmptyIdList_ReturnsEmptyWithoutQuerying()
    {
        var names = await CreateService().GetDisplayNamesAsync([], TestContext.Current.CancellationToken);

        Assert.Empty(names);
        _repository.VerifyNoOtherCalls();
        Assert.Empty(_telemetry.Activities);
    }

    [Fact]
    public async Task GetDisplayNamesAsync_RepositoryFails_MarksSpanAsErrorAndRethrows()
    {
        _repository
            .Setup(r => r.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("database down"));

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => CreateService().GetDisplayNamesAsync([Guid.CreateVersion7()], TestContext.Current.CancellationToken));

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Contains(activity.Events, e => e.Name == "exception");
    }

    [Fact]
    public async Task GetDisplayNamesAsync_Cancelled_DoesNotMarkSpanAsError()
    {
        _repository
            .Setup(r => r.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => CreateService().GetDisplayNamesAsync([Guid.CreateVersion7()], TestContext.Current.CancellationToken));

        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
    }

    [Fact]
    public async Task GetDisplayNamesAsync_NullIds_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateService().GetDisplayNamesAsync(null!, TestContext.Current.CancellationToken));
    }

    public void Dispose() => _telemetry.Dispose();

    private PlayerProfileService CreateService() => new(_repository.Object);
}
