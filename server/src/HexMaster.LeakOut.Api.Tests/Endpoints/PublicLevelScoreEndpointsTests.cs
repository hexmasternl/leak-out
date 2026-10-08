using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Endpoints;

public sealed class PublicLevelScoreEndpointsTests : IClassFixture<LeakOutApiFactory>
{
    private const string WebsiteOrigin = "https://leakout.hexmaster.nl";

    private readonly LeakOutApiFactory _factory;

    public PublicLevelScoreEndpointsTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.GetPublicLevelTopScoresHandler.Reset();
    }

    [Fact]
    public async Task GetPublicLevelTopScores_WithoutToken_Returns200WithTheWebsitesJsonShape()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(3,
        [
            new PublicLeaderboardEntryDto(1, "Bob", 600, new DateTimeOffset(2026, 10, 1, 14, 30, 0, TimeSpan.Zero)),
            new PublicLeaderboardEntryDto(2, "Ann", 540, new DateTimeOffset(2025, 3, 9, 8, 5, 12, TimeSpan.Zero)),
        ]));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/3/scores/top");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
        var root = json.RootElement;
        Assert.Equal(3, root.GetProperty("level").GetInt32());
        var first = root.GetProperty("entries")[0];
        Assert.Equal(1, first.GetProperty("position").GetInt32());
        Assert.Equal("Bob", first.GetProperty("displayName").GetString());
        Assert.Equal(600, first.GetProperty("score").GetInt32());
        // Nothing about the players beyond the display name.
        Assert.Equal(["position", "displayName", "score", "achievedAt"], first.EnumerateObject().Select(p => p.Name));
    }

    [Fact]
    public async Task GetPublicLevelTopScores_EveryEntryHasTheMomentInIso8601WithOffset()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(3,
        [
            new PublicLeaderboardEntryDto(1, "Bob", 600, new DateTimeOffset(2026, 10, 1, 14, 30, 0, TimeSpan.Zero)),
            new PublicLeaderboardEntryDto(2, "Ann", 540, new DateTimeOffset(2025, 3, 9, 8, 5, 12, TimeSpan.Zero)),
        ]));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/3/scores/top");

        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
        Assert.Equal(
            ["2026-10-01T14:30:00+00:00", "2025-03-09T08:05:12+00:00"],
            json.RootElement.GetProperty("entries").EnumerateArray().Select(e => e.GetProperty("achievedAt").GetString()));
    }

    [Fact]
    public async Task GetPublicLevelTopScores_LevelWithoutScores_Returns200WithEmptyEntries()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(7, []));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/7/scores/top");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
        Assert.Equal(7, json.RootElement.GetProperty("level").GetInt32());
        Assert.Equal(0, json.RootElement.GetProperty("entries").GetArrayLength());
    }

    [Fact]
    public async Task GetPublicLevelTopScores_PassesTheRouteLevelToTheHandler()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(42, []));

        await SendAsync(HttpMethod.Get, "/api/public/levels/42/scores/top");

        _factory.GetPublicLevelTopScoresHandler.Verify(
            h => h.Handle(new GetPublicLevelTopScoresQuery(42), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task GetPublicLevelTopScores_IsCacheableForAMinute()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(3, []));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/3/scores/top");

        Assert.Equal(TimeSpan.FromSeconds(60), response.Headers.CacheControl?.MaxAge);
        Assert.True(response.Headers.CacheControl?.Public);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-2)]
    public async Task GetPublicLevelTopScores_LevelBelowOne_Returns400NamingTheLevel(int level)
    {
        var response = await SendAsync(HttpMethod.Get, $"/api/public/levels/{level}/scores/top");

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
        Assert.Equal(["level"], json.RootElement.GetProperty("errors").EnumerateObject().Select(p => p.Name));
        _factory.GetPublicLevelTopScoresHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task GetPublicLevelTopScores_FromTheWebsite_AllowsThatOrigin()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(3, []));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/3/scores/top", origin: WebsiteOrigin);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal([WebsiteOrigin], response.Headers.GetValues("Access-Control-Allow-Origin"));
    }

    [Fact]
    public async Task GetPublicLevelTopScores_FromAnotherSite_DoesNotAllowThatOrigin()
    {
        HandlerReturns(new PublicLevelLeaderboardDto(3, []));

        var response = await SendAsync(HttpMethod.Get, "/api/public/levels/3/scores/top", origin: "https://evil.example");

        Assert.False(response.Headers.Contains("Access-Control-Allow-Origin"));
    }

    [Fact]
    public async Task GetPublicLevelTopScores_PreflightFromTheWebsite_IsAnsweredWithoutAToken()
    {
        var request = new HttpRequestMessage(HttpMethod.Options, "/api/public/levels/3/scores/top");
        request.Headers.Add("Origin", WebsiteOrigin);
        request.Headers.Add("Access-Control-Request-Method", "GET");
        request.Headers.Add("Access-Control-Request-Headers", "accept");

        var response = await _factory.CreateClient().SendAsync(request, TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.NoContent, response.StatusCode);
        Assert.Equal([WebsiteOrigin], response.Headers.GetValues("Access-Control-Allow-Origin"));
        Assert.Contains("GET", response.Headers.GetValues("Access-Control-Allow-Methods").Single());
        _factory.GetPublicLevelTopScoresHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task AuthenticatedLeaderboard_FromTheWebsite_DoesNotAllowThatOrigin()
    {
        var response = await SendAsync(HttpMethod.Get, "/api/levels/3/scores/top", origin: WebsiteOrigin);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.False(response.Headers.Contains("Access-Control-Allow-Origin"));
    }

    private void HandlerReturns(PublicLevelLeaderboardDto leaderboard) =>
        _factory.GetPublicLevelTopScoresHandler
            .Setup(h => h.Handle(It.IsAny<GetPublicLevelTopScoresQuery>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(leaderboard);

    private async Task<HttpResponseMessage> SendAsync(HttpMethod method, string path, string? origin = null)
    {
        var request = new HttpRequestMessage(method, path);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        if (origin is not null)
        {
            request.Headers.Add("Origin", origin);
        }

        return await _factory.CreateClient().SendAsync(request, TestContext.Current.CancellationToken);
    }
}
