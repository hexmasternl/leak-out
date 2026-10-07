using System.Net;
using System.Net.Http.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;
using HexMaster.LeakOut.Players.Features.SignInPlayer;
using HexMaster.LeakOut.Players.Services;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Endpoints;

public sealed class PlayerEndpointsTests : IClassFixture<LeakOutApiFactory>
{
    private const string SessionsPath = "/api/players/sessions";

    private readonly LeakOutApiFactory _factory;

    public PlayerEndpointsTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.SignInHandler.Reset();
    }

    [Fact]
    public async Task CreateSession_WithAcceptedCode_Returns200WithSession()
    {
        var session = new SessionDto("issued-token", DateTimeOffset.UtcNow.AddHours(1), Guid.CreateVersion7(), "Ann");
        _factory.SignInHandler
            .Setup(h => h.Handle(new SignInPlayerCommand("server-auth-code"), It.IsAny<CancellationToken>()))
            .ReturnsAsync(session);

        var response = await PostAsync(new { serverAuthCode = "server-auth-code" });

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken);
        var returned = await response.Content.ReadFromJsonAsync<SessionDto>(TestContext.Current.CancellationToken);
        Assert.Equal(session, returned);
        Assert.DoesNotContain(TestTokens.SubjectId, body);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData(null)]
    public async Task CreateSession_WithBlankCode_Returns400WithoutCallingHandler(string? code)
    {
        var response = await PostAsync(new { serverAuthCode = code });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Contains("serverAuthCode", await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
        _factory.SignInHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task CreateSession_WithoutBody_Returns400()
    {
        using var content = new StringContent(string.Empty, System.Text.Encoding.UTF8, "application/json");

        var response = await _factory.CreateClient().PostAsync(SessionsPath, content, TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        _factory.SignInHandler.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task CreateSession_WithRejectedCode_Returns401()
    {
        _factory.SignInHandler
            .Setup(h => h.Handle(It.IsAny<SignInPlayerCommand>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesAuthenticationException("rejected"));

        var response = await PostAsync(new { serverAuthCode = "used-code" });

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.DoesNotContain("accessToken", await response.Content.ReadAsStringAsync(TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task CreateSession_WhenGoogleIsUnavailable_Returns503()
    {
        _factory.SignInHandler
            .Setup(h => h.Handle(It.IsAny<SignInPlayerCommand>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new GooglePlayGamesUnavailableException("down"));

        var response = await PostAsync(new { serverAuthCode = "code" });

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
    }

    private Task<HttpResponseMessage> PostAsync(object body) =>
        _factory.CreateClient().PostAsJsonAsync(SessionsPath, body, TestContext.Current.CancellationToken);
}
