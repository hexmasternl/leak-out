using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.DailyPipes.Features.GetDailyTopScores;
using HexMaster.LeakOut.DailyPipes.Features.GetTodaysDailyPipe;
using HexMaster.LeakOut.DailyPipes.Features.SubmitDailyScore;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Endpoints;

public sealed class DailyPipeEndpointsTests : IClassFixture<LeakOutApiFactory>
{
    private const string TodayPath = "/api/daily-pipes/today";
    private const string ScoresPath = "/api/daily-pipes/2026-11-02/scores";
    private const string TopPath = "/api/daily-pipes/2026-11-02/scores/top";

    private static readonly DateOnly Date = new(2026, 11, 2);

    private readonly LeakOutApiFactory _factory;
    private readonly Guid _playerId = Guid.CreateVersion7();

    public DailyPipeEndpointsTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.GetTodaysDailyPipeHandler.Reset();
        _factory.SubmitDailyScoreHandler.Reset();
        _factory.GetDailyTopScoresHandler.Reset();
        _factory.UserResolver.Reset();
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(_playerId);
    }

    private static CancellationToken Ct => TestContext.Current.CancellationToken;

    [Theory]
    [InlineData("GET", TodayPath)]
    [InlineData("POST", ScoresPath)]
    [InlineData("GET", TopPath)]
    public async Task Endpoint_WithoutToken_Returns401(string method, string path)
    {
        var response = await SendAsync(method, path, accessToken: null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.GetTodaysDailyPipeHandler.VerifyNoOtherCalls();
        _factory.SubmitDailyScoreHandler.VerifyNoOtherCalls();
        _factory.GetDailyTopScoresHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Today_ForUnknownPlayer_Returns401()
    {
        var response = await SendAsync("GET", TodayPath, TestTokens.Create(subjectId: "g-unknown"));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Today_Published_Returns200WithTheLevelAsAnObject()
    {
        using var level = JsonDocument.Parse("""{"id":20261102,"name":"Daily Pipe 2026-11-02"}""");
        _factory.GetTodaysDailyPipeHandler
            .Setup(h => h.Handle(new GetTodaysDailyPipeQuery(_playerId), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new DailyPipeDto(Date, level.RootElement.Clone(), 1234, new DailyStreakDto(3, 7, false), null));

        var response = await SendAsync("GET", TodayPath, TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var body = JsonDocument.Parse(await response.Content.ReadAsStringAsync(Ct));
        Assert.Equal("2026-11-02", body.RootElement.GetProperty("date").GetString());
        Assert.Equal(20261102, body.RootElement.GetProperty("level").GetProperty("id").GetInt32());
        Assert.Equal(1234, body.RootElement.GetProperty("stackSeed").GetInt64());
        Assert.Equal(3, body.RootElement.GetProperty("streak").GetProperty("current").GetInt32());
        Assert.False(body.RootElement.GetProperty("streak").GetProperty("wonToday").GetBoolean());
        Assert.Equal(JsonValueKind.Null, body.RootElement.GetProperty("bestScore").ValueKind);
    }

    [Fact]
    public async Task Today_NotPublished_Returns404()
    {
        _factory.GetTodaysDailyPipeHandler
            .Setup(h => h.Handle(It.IsAny<GetTodaysDailyPipeQuery>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((DailyPipeDto?)null);

        var response = await SendAsync("GET", TodayPath, TestTokens.Create());

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task Submit_Accepted_Returns200WithTheResult()
    {
        var result = new DailyScoreResultDto(Date, new DailyBestScoreDto(380, 2), true, 4, new DailyStreakDto(4, 4, true));
        _factory.SubmitDailyScoreHandler
            .Setup(h => h.Handle(new SubmitDailyScoreCommand(_playerId, Date, 380, 2), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new SubmitDailyScoreOutcome.Accepted(result));

        var response = await SendAsync("POST", ScoresPath, TestTokens.Create(), new { score = 380, stars = 2 });

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(result, await response.Content.ReadFromJsonAsync<DailyScoreResultDto>(Ct));
    }

    [Fact]
    public async Task Submit_DayEnded_Returns409()
    {
        _factory.SubmitDailyScoreHandler
            .Setup(h => h.Handle(It.IsAny<SubmitDailyScoreCommand>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new SubmitDailyScoreOutcome.Ended());

        var response = await SendAsync("POST", ScoresPath, TestTokens.Create(), new { score = 380, stars = 2 });

        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
        using var body = JsonDocument.Parse(await response.Content.ReadAsStringAsync(Ct));
        Assert.Equal("This daily pipe has ended.", body.RootElement.GetProperty("title").GetString());
    }

    [Fact]
    public async Task Submit_ScoreAboveTheMaximum_Returns400NamingTheScore()
    {
        _factory.SubmitDailyScoreHandler
            .Setup(h => h.Handle(It.IsAny<SubmitDailyScoreCommand>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new SubmitDailyScoreOutcome.ScoreTooHigh(900));

        var response = await SendAsync("POST", ScoresPath, TestTokens.Create(), new { score = 5000, stars = 3 });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal(["score"], (await ProblemAsync(response))?.Errors.Keys);
    }

    [Theory]
    [InlineData(300, 4, "stars")]
    [InlineData(300, -1, "stars")]
    [InlineData(-5, 1, "score")]
    public async Task Submit_InvalidInput_Returns400NamingTheFieldWithoutCallingTheHandler(int score, int stars, string field)
    {
        var response = await SendAsync("POST", ScoresPath, TestTokens.Create(), new { score, stars });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal([field], (await ProblemAsync(response))?.Errors.Keys);
        _factory.SubmitDailyScoreHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Submit_MalformedDate_Returns400()
    {
        var response = await SendAsync("POST", "/api/daily-pipes/yesterday/scores", TestTokens.Create(), new { score = 1, stars = 1 });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        _factory.SubmitDailyScoreHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task Top_Known_Returns200()
    {
        var top = new DailyTopScoresDto(Date, [new DailyLeaderboardEntryDto(1, "Ada", 400, 3, false)], new DailyPlayerRankDto(2, 380, 2));
        _factory.GetDailyTopScoresHandler
            .Setup(h => h.Handle(new GetDailyTopScoresQuery(_playerId, Date), It.IsAny<CancellationToken>()))
            .ReturnsAsync(top);

        var response = await SendAsync("GET", TopPath, TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<DailyTopScoresDto>(Ct);
        Assert.Equal(top.Entries, body!.Entries);
        Assert.Equal(top.Player, body.Player);
    }

    [Fact]
    public async Task Top_UnknownDate_Returns404()
    {
        _factory.GetDailyTopScoresHandler
            .Setup(h => h.Handle(It.IsAny<GetDailyTopScoresQuery>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((DailyTopScoresDto?)null);

        var response = await SendAsync("GET", TopPath, TestTokens.Create());

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    private async Task<HttpResponseMessage> SendAsync(string method, string path, string? accessToken, object? body = null)
    {
        var client = _factory.CreateClient();
        if (accessToken is not null)
        {
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        }

        return method == "POST"
            ? await client.PostAsJsonAsync(path, body ?? new { score = 1, stars = 1 }, Ct)
            : await client.GetAsync(path, Ct);
    }

    private static Task<HttpValidationProblem?> ProblemAsync(HttpResponseMessage response) =>
        response.Content.ReadFromJsonAsync<HttpValidationProblem>(Ct);

    private sealed record HttpValidationProblem(Dictionary<string, string[]> Errors);
}
