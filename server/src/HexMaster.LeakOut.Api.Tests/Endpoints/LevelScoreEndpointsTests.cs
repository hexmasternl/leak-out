using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetLevelTopScores;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Endpoints;

public sealed class LevelScoreEndpointsTests : IClassFixture<LeakOutApiFactory>
{
    private readonly LeakOutApiFactory _factory;
    private readonly Guid _playerId = Guid.CreateVersion7();

    public LevelScoreEndpointsTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.GetLevelTopScoresHandler.Reset();
        _factory.UserResolver.Reset();
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(_playerId);
    }

    [Fact]
    public async Task GetLevelTopScores_Returns200WithLeaderboard()
    {
        var leaderboard = new LevelLeaderboardDto(3,
        [
            new LeaderboardEntryDto(1, "Bob", 600, IsCurrentPlayer: false),
            new LeaderboardEntryDto(2, "Ann", 540, IsCurrentPlayer: true),
        ]);
        _factory.GetLevelTopScoresHandler
            .Setup(h => h.Handle(new GetLevelTopScoresQuery(_playerId, 3), It.IsAny<CancellationToken>()))
            .ReturnsAsync(leaderboard);

        var response = await GetAsync("/api/levels/3/scores/top", TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<LevelLeaderboardDto>(TestContext.Current.CancellationToken);
        Assert.NotNull(body);
        Assert.Equal(3, body.Level);
        Assert.Equal(leaderboard.Entries, body.Entries);
    }

    [Fact]
    public async Task GetLevelTopScores_LevelWithoutScores_Returns200WithEmptyEntries()
    {
        _factory.GetLevelTopScoresHandler
            .Setup(h => h.Handle(new GetLevelTopScoresQuery(_playerId, 7), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new LevelLeaderboardDto(7, []));

        var response = await GetAsync("/api/levels/7/scores/top", TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<LevelLeaderboardDto>(TestContext.Current.CancellationToken);
        Assert.Empty(body!.Entries);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-2)]
    public async Task GetLevelTopScores_LevelBelowOne_Returns400NamingTheLevel(int level)
    {
        var response = await GetAsync($"/api/levels/{level}/scores/top", TestTokens.Create());

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var problem = await response.Content.ReadFromJsonAsync<HttpValidationProblem>(TestContext.Current.CancellationToken);
        Assert.Equal(["level"], problem?.Errors.Keys);
        _factory.GetLevelTopScoresHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task GetLevelTopScores_WithoutToken_Returns401WithoutCallingHandler()
    {
        var response = await GetAsync("/api/levels/3/scores/top", accessToken: null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.GetLevelTopScoresHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task GetLevelTopScores_ForUnknownPlayer_Returns401WithoutCallingHandler()
    {
        var response = await GetAsync("/api/levels/3/scores/top", TestTokens.Create(subjectId: "g-unknown"));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.GetLevelTopScoresHandler.VerifyNoOtherCalls();
    }

    private async Task<HttpResponseMessage> GetAsync(string path, string? accessToken)
    {
        var client = _factory.CreateClient();
        if (accessToken is not null)
        {
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        }

        return await client.GetAsync(path, TestContext.Current.CancellationToken);
    }

    private sealed record HttpValidationProblem(Dictionary<string, string[]> Errors);
}
