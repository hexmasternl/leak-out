using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Factories;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.SignInPlayer;

public sealed class SignInPlayerCommandHandlerTests
{
    private const string Code = "server-auth-code";
    private const string SubjectId = "g1234567890";
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);
    private static readonly AccessToken Token = new("issued-token", Now.AddHours(1));

    private readonly Mock<IGooglePlayGamesClient> _google = new();
    private readonly Mock<IPlayerRepository> _repository = new();
    private readonly Mock<IAccessTokenIssuer> _tokenIssuer = new();
    private readonly Mock<IClock> _clock = new();

    public SignInPlayerCommandHandlerTests()
    {
        _clock.Setup(c => c.UtcNow).Returns(Now);
        _tokenIssuer.Setup(i => i.Issue(SubjectId)).Returns(Token);
    }

    [Fact]
    public async Task Handle_NewPlayer_RegistersPlayerAndReturnsSession()
    {
        GoogleReturns("Ann");
        Player? added = null;
        _repository.Setup(r => r.AddAsync(SubjectId, It.IsAny<Player>(), It.IsAny<CancellationToken>()))
            .Callback<string, Player, CancellationToken>((_, player, _) => added = player)
            .Returns(Task.CompletedTask);

        var session = await CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken);

        _repository.Verify(r => r.AddAsync(SubjectId, It.IsAny<Player>(), It.IsAny<CancellationToken>()), Times.Once);
        _repository.Verify(r => r.UpdateAsync(It.IsAny<Player>(), It.IsAny<CancellationToken>()), Times.Never);
        Assert.NotNull(added);
        Assert.Equal("Ann", added.DisplayName);
        Assert.Equal(Now, added.CreatedAt);
        Assert.Equal(added.Id, session.PlayerId);
        Assert.Equal("Ann", session.DisplayName);
        Assert.Equal(Token.Token, session.AccessToken);
        Assert.Equal(Token.ExpiresAt, session.ExpiresAt);
    }

    [Fact]
    public async Task Handle_ReturningPlayer_RefreshesDisplayNameAndKeepsId()
    {
        var existing = new PlayerFaker("Ann").Generate();
        var playerId = existing.Id;
        GoogleReturns("Annie");
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>())).ReturnsAsync(existing);

        var session = await CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken);

        _repository.Verify(r => r.UpdateAsync(existing, It.IsAny<CancellationToken>()), Times.Once);
        _repository.Verify(r => r.AddAsync(It.IsAny<string>(), It.IsAny<Player>(), It.IsAny<CancellationToken>()), Times.Never);
        Assert.Equal("Annie", existing.DisplayName);
        Assert.Equal(Now, existing.LastSignedInAt);
        Assert.Equal(playerId, session.PlayerId);
        Assert.Equal("Annie", session.DisplayName);
    }

    [Fact]
    public async Task Handle_RejectedCode_ThrowsAndChangesNothing()
    {
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesAuthenticationException("rejected"));

        await Assert.ThrowsAsync<GooglePlayGamesAuthenticationException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        VerifyNothingStoredOrIssued();
    }

    [Fact]
    public async Task Handle_GoogleUnavailable_ThrowsAndChangesNothing()
    {
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesUnavailableException("down"));

        await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        VerifyNothingStoredOrIssued();
    }

    [Fact]
    public async Task Handle_NullCommand_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _google.VerifyNoOtherCalls();
    }

    private void GoogleReturns(string displayName) =>
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GooglePlayGamesPlayer(SubjectId, displayName));

    private void VerifyNothingStoredOrIssued()
    {
        _repository.VerifyNoOtherCalls();
        _tokenIssuer.VerifyNoOtherCalls();
    }

    private SignInPlayerCommandHandler CreateHandler() =>
        new(_google.Object, _repository.Object, _tokenIssuer.Object, _clock.Object);
}
