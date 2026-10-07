using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Scores.Features.SubmitScore;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Endpoints;

public sealed class ScoreEndpointsTests : IClassFixture<LeakOutApiFactory>
{
    private const string ScoresPath = "/api/scores";

    private readonly LeakOutApiFactory _factory;
    private readonly Guid _playerId = Guid.CreateVersion7();

    public ScoreEndpointsTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.SubmitScoreHandler.Reset();
        _factory.UserResolver.Reset();
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(_playerId);
    }

    [Fact]
    public async Task SubmitScore_ValidSubmission_Returns200WithHandlerResult()
    {
        var result = new SubmitScoreResult(4, 540, IsNewBest: true);
        _factory.SubmitScoreHandler
            .Setup(h => h.Handle(new SubmitScoreCommand(_playerId, 4, 540), It.IsAny<CancellationToken>()))
            .ReturnsAsync(result);

        var response = await PostAsync(new { level = 4, score = 540 }, TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(result, await response.Content.ReadFromJsonAsync<SubmitScoreResult>(TestContext.Current.CancellationToken));
        _factory.SubmitScoreHandler.Verify(
            h => h.Handle(new SubmitScoreCommand(_playerId, 4, 540), It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task SubmitScore_ZeroScore_IsAccepted()
    {
        _factory.SubmitScoreHandler
            .Setup(h => h.Handle(new SubmitScoreCommand(_playerId, 3, 0), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new SubmitScoreResult(3, 0, IsNewBest: true));

        var response = await PostAsync(new { level = 3, score = 0 }, TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task SubmitScore_WithoutToken_Returns401WithoutCallingHandler()
    {
        var response = await PostAsync(new { level = 4, score = 540 }, accessToken: null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.SubmitScoreHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task SubmitScore_ForUnknownPlayer_Returns401WithoutCallingHandler()
    {
        var response = await PostAsync(new { level = 4, score = 540 }, TestTokens.Create(subjectId: "g-unknown"));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.SubmitScoreHandler.VerifyNoOtherCalls();
    }

    [Theory]
    [InlineData(0, 100, "level")]
    [InlineData(-1, 100, "level")]
    [InlineData(3, -5, "score")]
    public async Task SubmitScore_InvalidInput_Returns400NamingTheField(int level, int score, string field)
    {
        var response = await PostAsync(new { level, score }, TestTokens.Create());

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var problem = await response.Content.ReadFromJsonAsync<HttpValidationProblem>(TestContext.Current.CancellationToken);
        Assert.Equal([field], problem?.Errors.Keys);
        _factory.SubmitScoreHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task SubmitScore_InvalidLevelAndScore_Returns400NamingBothFields()
    {
        var response = await PostAsync(new { level = 0, score = -1 }, TestTokens.Create());

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var problem = await response.Content.ReadFromJsonAsync<HttpValidationProblem>(TestContext.Current.CancellationToken);
        Assert.Equal(["level", "score"], problem?.Errors.Keys.Order());
    }

    private async Task<HttpResponseMessage> PostAsync(object body, string? accessToken)
    {
        var client = _factory.CreateClient();
        if (accessToken is not null)
        {
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        }

        return await client.PostAsJsonAsync(ScoresPath, body, TestContext.Current.CancellationToken);
    }

    private sealed record HttpValidationProblem(Dictionary<string, string[]> Errors);
}
