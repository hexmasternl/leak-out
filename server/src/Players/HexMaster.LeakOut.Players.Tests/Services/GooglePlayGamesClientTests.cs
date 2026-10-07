using System.Net;
using System.Text;
using HexMaster.LeakOut.Players.Services;
using HexMaster.LeakOut.Players.Tests.Helpers;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace HexMaster.LeakOut.Players.Tests.Services;

public sealed class GooglePlayGamesClientTests
{
    private const string TokenJson = """{"access_token":"google-access-token","expires_in":3599,"token_type":"Bearer"}""";
    private const string PlayerJson = """{"kind":"games#player","playerId":"g1234567890","displayName":"Ann"}""";

    [Fact]
    public async Task GetPlayerAsync_ExchangesCodeAndReturnsPlayer()
    {
        var handler = new StubHttpMessageHandler(request => request.RequestUri == GooglePlayGamesClient.TokenEndpoint
            ? Json(HttpStatusCode.OK, TokenJson)
            : Json(HttpStatusCode.OK, PlayerJson));

        var player = await CreateClient(handler).GetPlayerAsync("server-auth-code", TestContext.Current.CancellationToken);

        Assert.Equal(new GooglePlayGamesPlayer("g1234567890", "Ann"), player);
        Assert.Equal(2, handler.Requests.Count);

        var exchange = handler.Requests[0];
        Assert.Equal(HttpMethod.Post, exchange.Method);
        Assert.Equal(GooglePlayGamesClient.TokenEndpoint, exchange.Uri);
        Assert.Contains("grant_type=authorization_code", exchange.Body);
        Assert.Contains("code=server-auth-code", exchange.Body);
        Assert.Contains("client_id=web-client-id", exchange.Body);
        Assert.Contains("client_secret=web-client-secret", exchange.Body);
        Assert.Contains("redirect_uri=", exchange.Body);

        var me = handler.Requests[1];
        Assert.Equal(HttpMethod.Get, me.Method);
        Assert.Equal(GooglePlayGamesClient.CurrentPlayerEndpoint, me.Uri);
        Assert.Equal("Bearer google-access-token", me.Authorization);
    }

    [Fact]
    public async Task GetPlayerAsync_WithoutDisplayName_ReturnsEmptyName()
    {
        var handler = new StubHttpMessageHandler(request => request.RequestUri == GooglePlayGamesClient.TokenEndpoint
            ? Json(HttpStatusCode.OK, TokenJson)
            : Json(HttpStatusCode.OK, """{"playerId":"g1"}"""));

        var player = await CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken);

        Assert.Equal(string.Empty, player.DisplayName);
    }

    [Fact]
    public async Task GetPlayerAsync_WhenGoogleRejectsCode_ThrowsAuthenticationException()
    {
        var handler = new StubHttpMessageHandler(_ => Json(HttpStatusCode.BadRequest, """{"error":"invalid_grant","error_description":"Bad Request"}"""));

        await Assert.ThrowsAsync<GooglePlayGamesAuthenticationException>(
            () => CreateClient(handler).GetPlayerAsync("used-code", TestContext.Current.CancellationToken));
        Assert.Single(handler.Requests);
    }

    [Theory]
    [InlineData(HttpStatusCode.InternalServerError)]
    [InlineData(HttpStatusCode.ServiceUnavailable)]
    [InlineData(HttpStatusCode.Unauthorized)]
    public async Task GetPlayerAsync_WhenTokenEndpointFails_ThrowsUnavailableException(HttpStatusCode status)
    {
        var handler = new StubHttpMessageHandler(_ => new HttpResponseMessage(status));

        await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task GetPlayerAsync_WhenGoogleIsUnreachable_ThrowsUnavailableException()
    {
        var handler = new StubHttpMessageHandler(_ => throw new HttpRequestException("No such host"));

        var exception = await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken));
        Assert.IsType<HttpRequestException>(exception.InnerException);
    }

    [Fact]
    public async Task GetPlayerAsync_WhenCancelled_ThrowsOperationCanceledException()
    {
        using var cancellation = new CancellationTokenSource();
        await cancellation.CancelAsync();
        var handler = new StubHttpMessageHandler(_ => Json(HttpStatusCode.OK, TokenJson));

        await Assert.ThrowsAnyAsync<OperationCanceledException>(
            () => CreateClient(handler).GetPlayerAsync("code", cancellation.Token));
    }

    [Theory]
    [InlineData("not json")]
    [InlineData("""{"token_type":"Bearer"}""")]
    public async Task GetPlayerAsync_WhenTokenResponseIsUnusable_ThrowsUnavailableException(string body)
    {
        var handler = new StubHttpMessageHandler(_ => Json(HttpStatusCode.OK, body));

        await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task GetPlayerAsync_WhenPlayersApiRejectsToken_ThrowsAuthenticationException()
    {
        var handler = new StubHttpMessageHandler(request => request.RequestUri == GooglePlayGamesClient.TokenEndpoint
            ? Json(HttpStatusCode.OK, TokenJson)
            : new HttpResponseMessage(HttpStatusCode.Unauthorized));

        await Assert.ThrowsAsync<GooglePlayGamesAuthenticationException>(
            () => CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken));
    }

    [Theory]
    [InlineData(HttpStatusCode.InternalServerError, "")]
    [InlineData(HttpStatusCode.OK, """{"displayName":"Ann"}""")]
    public async Task GetPlayerAsync_WhenPlayersApiFails_ThrowsUnavailableException(HttpStatusCode status, string body)
    {
        var handler = new StubHttpMessageHandler(request => request.RequestUri == GooglePlayGamesClient.TokenEndpoint
            ? Json(HttpStatusCode.OK, TokenJson)
            : Json(status, body));

        await Assert.ThrowsAsync<GooglePlayGamesUnavailableException>(
            () => CreateClient(handler).GetPlayerAsync("code", TestContext.Current.CancellationToken));
    }

    [Theory]
    [InlineData("")]
    [InlineData(" ")]
    public async Task GetPlayerAsync_WithBlankCode_ThrowsArgumentException(string code)
    {
        var handler = new StubHttpMessageHandler(_ => Json(HttpStatusCode.OK, TokenJson));

        await Assert.ThrowsAsync<ArgumentException>(
            () => CreateClient(handler).GetPlayerAsync(code, TestContext.Current.CancellationToken));
        Assert.Empty(handler.Requests);
    }

    private static GooglePlayGamesClient CreateClient(StubHttpMessageHandler handler) => new(
        new HttpClient(handler),
        Options.Create(new GooglePlayGamesOptions { ClientId = "web-client-id", ClientSecret = "web-client-secret" }),
        NullLogger<GooglePlayGamesClient>.Instance);

    private static HttpResponseMessage Json(HttpStatusCode status, string body) => new(status)
    {
        Content = new StringContent(body, Encoding.UTF8, "application/json"),
    };
}
