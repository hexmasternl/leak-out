using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using HexMaster.LeakOut.Api.Tests.Helpers;
using Microsoft.AspNetCore.Hosting;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Authentication;

public sealed class AccessTokenAuthenticationTests : IClassFixture<LeakOutApiFactory>
{
    private readonly LeakOutApiFactory _factory;

    public AccessTokenAuthenticationTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.UserResolver.Reset();
    }

    [Fact]
    public async Task ProtectedEndpoint_WithoutToken_Returns401()
    {
        var response = await _factory.CreateClient().GetAsync(LeakOutApiFactory.ProtectedPath, TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ProtectedEndpoint_WithExpiredToken_Returns401()
    {
        var token = TestTokens.Create(expiresAt: DateTimeOffset.UtcNow.AddMinutes(-2));

        var response = await GetProtectedAsync(token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ProtectedEndpoint_WithTokenSignedByAnotherKey_Returns401()
    {
        var token = TestTokens.Create(signingKey: "someone-elses-signing-key-that-is-long-enough");

        var response = await GetProtectedAsync(token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Theory]
    [InlineData("someone-else", "leak-out-game")]
    [InlineData("leak-out-server", "another-game")]
    public async Task ProtectedEndpoint_WithTokenForAnotherIssuerOrAudience_Returns401(string issuer, string audience)
    {
        var response = await GetProtectedAsync(TestTokens.Create(issuer: issuer, audience: audience));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ProtectedEndpoint_WithValidTokenOfRegisteredPlayer_IsHandledForInternalPlayerId()
    {
        var playerId = Guid.CreateVersion7();
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(playerId);

        var response = await GetProtectedAsync(TestTokens.Create());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(playerId, await response.Content.ReadFromJsonAsync<Guid>(TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task ProtectedEndpoint_WithTokenWithinClockSkew_IsAccepted()
    {
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(Guid.CreateVersion7());

        var response = await GetProtectedAsync(TestTokens.Create(expiresAt: DateTimeOffset.UtcNow.AddSeconds(-20)));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task ProtectedEndpoint_WithValidTokenOfUnknownPlayer_Returns401()
    {
        _factory.UserResolver
            .Setup(r => r.ResolveAsync(TestTokens.SubjectId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Guid?)null);

        var response = await GetProtectedAsync(TestTokens.Create());

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ProtectedEndpoint_WithValidTokenWithoutSubject_Returns401WithoutResolving()
    {
        var response = await GetProtectedAsync(TestTokens.Create(subjectId: null));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        _factory.UserResolver.VerifyNoOtherCalls();
    }

    [Theory]
    [InlineData("/health")]
    [InlineData("/alive")]
    [InlineData("/openapi/v1.json")]
    public async Task AnonymousEndpoints_WithoutToken_Return200(string path)
    {
        var response = await _factory.CreateClient().GetAsync(path, TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    // Azure Container Apps probes /alive (startup, liveness) and /health (readiness) in Production;
    // without them every revision would be marked unhealthy.
    [Theory]
    [InlineData("/health")]
    [InlineData("/alive")]
    public async Task ProbeEndpoints_InProduction_Return200WithoutToken(string path)
    {
        using var production = _factory.WithWebHostBuilder(builder => builder.UseEnvironment("Production"));

        var response = await production.CreateClient().GetAsync(path, TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task OpenApiDocument_InProduction_IsNotServedAnonymously()
    {
        using var production = _factory.WithWebHostBuilder(builder => builder.UseEnvironment("Production"));

        var response = await production.CreateClient().GetAsync("/openapi/v1.json", TestContext.Current.CancellationToken);

        Assert.NotEqual(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task OpenApiDocument_DescribesBearerSecurityScheme()
    {
        var document = await _factory.CreateClient().GetStringAsync("/openapi/v1.json", TestContext.Current.CancellationToken);

        Assert.Contains("\"bearer\"", document);
        Assert.Contains("\"JWT\"", document);
        Assert.Contains("CreateSession", document);
    }

    private Task<HttpResponseMessage> GetProtectedAsync(string token)
    {
        var client = _factory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return client.GetAsync(LeakOutApiFactory.ProtectedPath, TestContext.Current.CancellationToken);
    }
}
