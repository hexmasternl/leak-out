using Bogus;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests.GetLevelTopScores;

public sealed class GetLevelTopScoresQueryHandlerTests
{
    private const int Level = 3;

    private readonly Faker _faker = new();
    private readonly Mock<ILevelScoreRepository> _repository = new();
    private readonly Mock<IPlayerProfileService> _playerProfiles = new();
    private readonly Guid _requestingPlayer = Guid.CreateVersion7();

    [Fact]
    public async Task Handle_RankedScoresWithTies_KeepsRankingOrderAndPositions()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 600),
            new RankedLevelScore(2, Guid.CreateVersion7(), 500),
            new RankedLevelScore(3, Guid.CreateVersion7(), 500),
            new RankedLevelScore(4, Guid.CreateVersion7(), 500),
        };
        var names = RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(Level, result.Level);
        Assert.Equal(
            ranked.Select(s => new LeaderboardEntryDto(s.Position, names[s.UserId], s.Score, IsCurrentPlayer: false)),
            result.Entries);
    }

    [Fact]
    public async Task Handle_AsksRepositoryForTheTopTen()
    {
        RepositoryReturns([]);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        _repository.Verify(r => r.GetRankedAsync(Level, 0, 10, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_FewerThanTenScores_ReturnsThemAll()
    {
        var ranked = Enumerable.Range(1, 4)
            .Select(p => new RankedLevelScore(p, Guid.CreateVersion7(), 1000 - p * 10))
            .ToArray();
        RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal([1, 2, 3, 4], result.Entries.Select(e => e.Position));
    }

    [Fact]
    public async Task Handle_LevelWithoutScores_ReturnsEmptyEntriesWithoutNameLookup()
    {
        RepositoryReturns([]);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, 7), TestContext.Current.CancellationToken);

        Assert.Equal(7, result.Level);
        Assert.Empty(result.Entries);
        _playerProfiles.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Handle_RequestingPlayerRanked_MarksOnlyTheirEntry()
    {
        var ranked = new[]
        {
            new RankedLevelScore(1, Guid.CreateVersion7(), 900),
            new RankedLevelScore(2, _requestingPlayer, 800),
            new RankedLevelScore(3, Guid.CreateVersion7(), 700),
        };
        RepositoryReturns(ranked);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(2, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
    }

    [Fact]
    public async Task Handle_LooksUpAllNamesInOneBatch()
    {
        var ranked = Enumerable.Range(1, 10)
            .Select(p => new RankedLevelScore(p, Guid.CreateVersion7(), 1000 - p))
            .ToArray();
        RepositoryReturns(ranked);

        await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        _playerProfiles.Verify(
            p => p.GetDisplayNamesAsync(
                It.Is<IReadOnlyCollection<Guid>>(ids => ids.SequenceEqual(ranked.Select(s => s.UserId))),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("  ")]
    public async Task Handle_PlayerWithoutDisplayName_ShowsUnknownPlayer(string? displayName)
    {
        var known = new RankedLevelScore(1, Guid.CreateVersion7(), 900);
        var nameless = new RankedLevelScore(2, Guid.CreateVersion7(), 800);
        var names = new Dictionary<Guid, string> { [known.UserId] = "Ann" };
        if (displayName is not null)
        {
            names[nameless.UserId] = displayName;
        }

        RepositoryReturns([known, nameless], names);

        var result = await CreateHandler().Handle(new GetLevelTopScoresQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

        Assert.Equal(["Ann", "Unknown player"], result.Entries.Select(e => e.DisplayName));
    }

    [Fact]
    public async Task Handle_NullQuery_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _repository.VerifyNoOtherCalls();
        _playerProfiles.VerifyNoOtherCalls();
    }

    private Dictionary<Guid, string> RepositoryReturns(RankedLevelScore[] ranked, Dictionary<Guid, string>? names = null)
    {
        names ??= ranked.ToDictionary(s => s.UserId, _ => _faker.Internet.UserName());
        _repository
            .Setup(r => r.GetRankedAsync(It.IsAny<int>(), It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(ranked);
        _playerProfiles
            .Setup(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(names);
        return names;
    }

    private GetLevelTopScoresQueryHandler CreateHandler() => new(_repository.Object, _playerProfiles.Object);
}
