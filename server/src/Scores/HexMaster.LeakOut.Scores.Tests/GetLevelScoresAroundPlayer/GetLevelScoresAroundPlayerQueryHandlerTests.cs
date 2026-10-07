using Bogus;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;
using Moq;

namespace HexMaster.LeakOut.Scores.Tests.GetLevelScoresAroundPlayer;

public sealed class GetLevelScoresAroundPlayerQueryHandlerTests
{
    private const int Level = 3;

    private static readonly DateTimeOffset AchievedAt = new(2026, 10, 1, 12, 0, 0, TimeSpan.Zero);

    private readonly Faker _faker = new();
    private readonly Mock<ILevelScoreRepository> _repository = new();
    private readonly Mock<IPlayerProfileService> _playerProfiles = new();
    private readonly Guid _requestingPlayer = Guid.CreateVersion7();

    [Fact]
    public async Task Handle_PlayerInTheMiddle_ReturnsFiveAboveAndFourBelow()
    {
        var ranking = RankingWithPlayerAt(128, total: 200);

        var result = await HandleAsync();

        Assert.Equal(Level, result.Level);
        Assert.Equal(Enumerable.Range(123, 10), result.Entries.Select(e => e.Position));
        Assert.Equal(128, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
        Assert.Equal(ranking.Skip(122).Take(10).Select(s => s.Score), result.Entries.Select(e => e.Score));
        _repository.Verify(r => r.GetRankedAsync(Level, 122, 10, It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task Handle_PlayerNearTheTop_ShiftsTheWindowDown()
    {
        RankingWithPlayerAt(3, total: 50);

        var result = await HandleAsync();

        Assert.Equal(Enumerable.Range(1, 10), result.Entries.Select(e => e.Position));
        Assert.Equal(3, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
    }

    [Fact]
    public async Task Handle_PlayerNearTheBottom_ShiftsTheWindowUp()
    {
        RankingWithPlayerAt(49, total: 50);

        var result = await HandleAsync();

        Assert.Equal(Enumerable.Range(41, 10), result.Entries.Select(e => e.Position));
        Assert.Equal(49, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
    }

    [Fact]
    public async Task Handle_LevelWithFewerThanTenScores_ReturnsThemAll()
    {
        RankingWithPlayerAt(4, total: 6);

        var result = await HandleAsync();

        Assert.Equal(Enumerable.Range(1, 6), result.Entries.Select(e => e.Position));
        Assert.Equal(4, Assert.Single(result.Entries, e => e.IsCurrentPlayer).Position);
    }

    [Fact]
    public async Task Handle_CountsAboveThePlayersStoredScore()
    {
        RankingWithPlayerAt(10, total: 20, playerScore: 526);

        await HandleAsync();

        _repository.Verify(
            r => r.CountRankedAboveAsync(Level, 526, AchievedAt, _requestingPlayer, It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task Handle_ScoreAddedBetweenTheReads_StillIncludesThePlayer()
    {
        RankingWithPlayerAt(10, total: 10);
        _repository
            .Setup(r => r.CountRankedAboveAsync(Level, It.IsAny<int>(), It.IsAny<DateTimeOffset>(), _requestingPlayer, It.IsAny<CancellationToken>()))
            .ReturnsAsync(10);

        var result = await HandleAsync();

        _repository.Verify(r => r.GetRankedAsync(Level, 1, 10, It.IsAny<CancellationToken>()), Times.Once);
        Assert.Equal(Level, result.Level);
    }

    [Fact]
    public async Task Handle_PlayerWithoutScore_ReturnsEmptyEntriesWithoutReadingTheWindow()
    {
        _repository
            .Setup(r => r.GetBestAsync(_requestingPlayer, 7, It.IsAny<CancellationToken>()))
            .ReturnsAsync((BestLevelScore?)null);

        var result = await CreateHandler().Handle(
            new GetLevelScoresAroundPlayerQuery(_requestingPlayer, 7), TestContext.Current.CancellationToken);

        Assert.Equal(7, result.Level);
        Assert.Empty(result.Entries);
        _repository.Verify(r => r.GetBestAsync(_requestingPlayer, 7, It.IsAny<CancellationToken>()), Times.Once);
        _repository.VerifyNoOtherCalls();
        _playerProfiles.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Handle_LooksUpTheWindowsNamesInOneBatch()
    {
        var ranking = RankingWithPlayerAt(128, total: 200);

        await HandleAsync();

        _playerProfiles.Verify(
            p => p.GetDisplayNamesAsync(
                It.Is<IReadOnlyCollection<Guid>>(ids => ids.SequenceEqual(ranking.Skip(122).Take(10).Select(s => s.UserId))),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("  ")]
    public async Task Handle_PlayerWithoutDisplayName_ShowsUnknownPlayer(string? displayName)
    {
        var ranking = RankingWithPlayerAt(1, total: 2);
        var names = new Dictionary<Guid, string> { [_requestingPlayer] = "Ann" };
        if (displayName is not null)
        {
            names[ranking[1].UserId] = displayName;
        }

        _playerProfiles
            .Setup(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(names);

        var result = await HandleAsync();

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

    /// <summary>
    /// Sets up a ranking of <paramref name="total"/> scores on <see cref="Level"/> with the requesting
    /// player at <paramref name="position"/>, served by the mocked repository reads.
    /// </summary>
    private RankedLevelScore[] RankingWithPlayerAt(int position, int total, int playerScore = 5_000)
    {
        var ranking = Enumerable.Range(1, total)
            .Select(p => new RankedLevelScore(
                p,
                p == position ? _requestingPlayer : Guid.CreateVersion7(),
                p == position ? playerScore : playerScore + (position - p) * 10))
            .ToArray();
        var names = ranking.ToDictionary(s => s.UserId, _ => _faker.Internet.UserName());

        _repository
            .Setup(r => r.GetBestAsync(_requestingPlayer, Level, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new BestLevelScore(playerScore, AchievedAt));
        _repository
            .Setup(r => r.CountRankedAboveAsync(Level, playerScore, AchievedAt, _requestingPlayer, It.IsAny<CancellationToken>()))
            .ReturnsAsync(position - 1);
        _repository
            .Setup(r => r.CountAsync(Level, It.IsAny<CancellationToken>()))
            .ReturnsAsync(total);
        _repository
            .Setup(r => r.GetRankedAsync(Level, It.IsAny<int>(), It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((int _, int offset, int count, CancellationToken _) => ranking.Skip(offset).Take(count).ToList());
        _playerProfiles
            .Setup(p => p.GetDisplayNamesAsync(It.IsAny<IReadOnlyCollection<Guid>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(names);

        return ranking;
    }

    private Task<LevelLeaderboardDto> HandleAsync() =>
        CreateHandler().Handle(new GetLevelScoresAroundPlayerQuery(_requestingPlayer, Level), TestContext.Current.CancellationToken);

    private GetLevelScoresAroundPlayerQueryHandler CreateHandler() => new(_repository.Object, _playerProfiles.Object);
}
