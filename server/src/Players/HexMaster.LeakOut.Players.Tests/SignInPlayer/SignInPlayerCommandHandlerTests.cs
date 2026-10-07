using System.Diagnostics;
using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Observability;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Factories;
using HexMaster.LeakOut.Players.Tests.Helpers;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Testing;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.SignInPlayer;

[Collection(TelemetryCollection.Name)]
public sealed class SignInPlayerCommandHandlerTests : IDisposable
{
    private const string Code = "server-auth-code";
    private const string SubjectId = "g1234567890";
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, TimeSpan.Zero);
    private static readonly AccessToken Token = new("issued-token", Now.AddHours(1));

    private readonly Mock<IGooglePlayGamesClient> _google = new();
    private readonly Mock<IPlayerRepository> _repository = new();
    private readonly Mock<IAccessTokenIssuer> _tokenIssuer = new();
    private readonly Mock<IClock> _clock = new();
    private readonly TelemetryCollector _telemetry = new();
    private readonly FakeLogger<SignInPlayerCommandHandler> _logger = new();

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
        AssertSignInRecorded(PlayersMetrics.OutcomeSuccess, newPlayer: true);

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Information, log.Level);
        Assert.Equal($"Player {added.Id} registered", log.Message);
    }

    [Fact]
    public async Task Handle_NewPlayer_KeepsGoogleIdentityCodeTokenAndNameOutOfTelemetry()
    {
        GoogleReturns("Ann");

        await CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken);

        string[] secrets = [SubjectId, Code, Token.Token, "Ann"];
        var values = _telemetry.Activities.SelectMany(a => a.TagObjects.Select(t => t.Value?.ToString()))
            .Concat(_telemetry.Measurements.SelectMany(m => m.Tags.Values.Select(v => v?.ToString())))
            .Concat(_telemetry.DoubleMeasurements.SelectMany(m => m.Tags.Values.Select(v => v?.ToString())))
            .Concat(_logger.Collector.GetSnapshot().Select(l => l.Message))
            .ToList();
        Assert.NotEmpty(values);
        Assert.DoesNotContain(values, v => v is not null && secrets.Any(v.Contains));
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
        AssertSignInRecorded(PlayersMetrics.OutcomeSuccess, newPlayer: false);

        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(PlayersMetrics.SignInActivityName, activity.OperationName);
        Assert.Equal(ActivityStatusCode.Ok, activity.Status);
        Assert.Equal(playerId, activity.GetTagItem("player.id"));
        Assert.Equal(false, activity.GetTagItem("player.new"));
        AssertSubjectIdAndTokenNotInTelemetry();
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_RejectedCode_ThrowsAndChangesNothing()
    {
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesAuthenticationException("rejected"));

        await Assert.ThrowsAsync<GooglePlayGamesAuthenticationException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        VerifyNothingStoredOrIssued();
        AssertSignInRecorded(PlayersMetrics.OutcomeRejected, newPlayer: null);
        Assert.Equal(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        // GooglePlayGamesClient logs the rejection; the handler does not log it twice.
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_GoogleUnavailable_ThrowsAndChangesNothing()
    {
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesUnavailableException("down"));

        await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        VerifyNothingStoredOrIssued();
        AssertSignInRecorded(PlayersMetrics.OutcomeUnavailable, newPlayer: null);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_RepositoryFails_RecordsErrorAndRethrows()
    {
        GoogleReturns("Ann");
        var failure = new InvalidOperationException("database down");
        _repository.Setup(r => r.FindBySubjectIdAsync(SubjectId, It.IsAny<CancellationToken>()))
            .ThrowsAsync(failure);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        AssertSignInRecorded(PlayersMetrics.OutcomeError, newPlayer: null);
        var activity = Assert.Single(_telemetry.Activities);
        Assert.Equal(ActivityStatusCode.Error, activity.Status);
        Assert.Contains(activity.Events, e => e.Name == "exception");

        var log = Assert.Single(_logger.Collector.GetSnapshot());
        Assert.Equal(LogLevel.Error, log.Level);
        Assert.Same(failure, log.Exception);
    }

    [Fact]
    public async Task Handle_Cancelled_RecordsNoOutcomeAndNoError()
    {
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new OperationCanceledException());

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => CreateHandler().Handle(new SignInPlayerCommand(Code), TestContext.Current.CancellationToken));

        Assert.Empty(_telemetry.Measurements);
        Assert.Empty(_telemetry.DoubleMeasurements);
        Assert.NotEqual(ActivityStatusCode.Error, Assert.Single(_telemetry.Activities).Status);
        Assert.Empty(_logger.Collector.GetSnapshot());
    }

    [Fact]
    public async Task Handle_NullCommand_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => CreateHandler().Handle(null!, TestContext.Current.CancellationToken));

        _google.VerifyNoOtherCalls();
    }

    public void Dispose() => _telemetry.Dispose();

    private void GoogleReturns(string displayName) =>
        _google.Setup(g => g.GetPlayerAsync(Code, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new GooglePlayGamesPlayer(SubjectId, displayName));

    private void VerifyNothingStoredOrIssued()
    {
        _repository.VerifyNoOtherCalls();
        _tokenIssuer.VerifyNoOtherCalls();
    }

    private void AssertSignInRecorded(string outcome, bool? newPlayer)
    {
        var measurement = Assert.Single(_telemetry.Measurements);
        Assert.Equal(PlayersMetrics.SignInsInstrumentName, measurement.Instrument);
        Assert.Equal(1, measurement.Value);
        Assert.Equal(outcome, measurement.Tags["outcome"]);
        if (newPlayer is null)
        {
            Assert.False(measurement.Tags.ContainsKey("new_player"));
        }
        else
        {
            Assert.Equal(newPlayer, measurement.Tags["new_player"]);
        }

        var duration = Assert.Single(_telemetry.DoubleMeasurements);
        Assert.Equal(PlayersMetrics.SignInDurationInstrumentName, duration.Instrument);
        Assert.True(duration.Value >= 0);
        Assert.Equal(outcome, Assert.Single(duration.Tags).Value);
    }

    private void AssertSubjectIdAndTokenNotInTelemetry()
    {
        Assert.DoesNotContain(_telemetry.Measurements, m => m.Tags.Values.Contains(SubjectId));
        Assert.DoesNotContain(
            _telemetry.Activities,
            a => a.TagObjects.Any(t => Equals(t.Value, SubjectId) || Equals(t.Value, Token.Token)));
    }

    private SignInPlayerCommandHandler CreateHandler() =>
        new(_google.Object, _repository.Object, _tokenIssuer.Object, _clock.Object, _telemetry.Metrics, _logger);
}
