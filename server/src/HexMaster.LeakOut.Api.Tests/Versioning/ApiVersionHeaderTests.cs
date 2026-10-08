using System.Net;
using System.Reflection;
using HexMaster.LeakOut.Api.Tests.Helpers;
using HexMaster.LeakOut.Api.Versioning;
using HexMaster.LeakOut.Scores.Features.GetPublicLevelTopScores;
using Moq;

namespace HexMaster.LeakOut.Api.Tests.Versioning;

public sealed class ApiVersionHeaderTests : IClassFixture<LeakOutApiFactory>
{
    private readonly LeakOutApiFactory _factory;

    public ApiVersionHeaderTests(LeakOutApiFactory factory)
    {
        _factory = factory;
        _factory.GetPublicLevelTopScoresHandler.Reset();
    }

    [Fact]
    public void Version_IsTheInformationalVersionOfTheApiAssembly()
    {
        var expected = typeof(Program).Assembly
            .GetCustomAttribute<AssemblyInformationalVersionAttribute>()!.InformationalVersion;

        Assert.Equal(expected, ApiVersionHeader.Version);
    }

    [Fact]
    public void Version_HasNoCommitShaAppendedByTheSdk()
    {
        // The deploy workflow sets the whole value; the SDK must not add "+<full sha>" itself.
        Assert.DoesNotMatch("[0-9a-f]{40}", ApiVersionHeader.Version);
    }

    [Theory]
    [InlineData("/alive", HttpStatusCode.OK)]
    [InlineData("/api/public/levels/0/scores/top", HttpStatusCode.BadRequest)]
    [InlineData("/api/levels/1/scores/around-me", HttpStatusCode.Unauthorized)]
    public async Task EveryResponse_CarriesTheApiVersionHeader(string path, HttpStatusCode expectedStatus)
    {
        using var client = _factory.CreateClient();

        var response = await client.GetAsync(path, TestContext.Current.CancellationToken);

        Assert.Equal(expectedStatus, response.StatusCode);
        Assert.Equal([ApiVersionHeader.Version], response.Headers.GetValues(ApiVersionHeader.HeaderName));
    }

    [Fact]
    public async Task ServerError_CarriesTheApiVersionHeader()
    {
        _factory.GetPublicLevelTopScoresHandler
            .Setup(h => h.Handle(It.IsAny<GetPublicLevelTopScoresQuery>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new InvalidOperationException("database down"));
        using var client = _factory.CreateClient();

        var response = await client.GetAsync("/api/public/levels/1/scores/top", TestContext.Current.CancellationToken);

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
        Assert.Equal([ApiVersionHeader.Version], response.Headers.GetValues(ApiVersionHeader.HeaderName));
    }
}
