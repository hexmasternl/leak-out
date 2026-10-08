using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Services;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;
using Moq;

namespace HexMaster.LeakOut.Players.Tests.Services;

public sealed class JwtAccessTokenIssuerTests
{
    private const string SigningKey = "a-test-signing-key-that-is-long-enough-for-hs256";
    private static readonly DateTimeOffset Now = new(2026, 10, 7, 12, 0, 0, 250, TimeSpan.Zero);

    private readonly AccessTokenOptions _options = new() { SigningKey = SigningKey };
    private readonly Mock<IClock> _clock = new();

    public JwtAccessTokenIssuerTests()
    {
        _clock.Setup(c => c.UtcNow).Returns(Now);
    }

    [Fact]
    public async Task Issue_CreatesTokenSignedWithConfiguredKeyAndClaims()
    {
        var accessToken = CreateIssuer().Issue("g1234567890");

        var result = await new JsonWebTokenHandler().ValidateTokenAsync(accessToken.Token, new TokenValidationParameters
        {
            ValidIssuer = "leak-out-server",
            ValidAudience = "leak-out-game",
            IssuerSigningKey = _options.CreateSigningKey(),
            ValidateLifetime = false,
        });

        Assert.True(result.IsValid, result.Exception?.Message);
        var token = (JsonWebToken)result.SecurityToken;
        Assert.Equal("g1234567890", token.Subject);
        Assert.False(string.IsNullOrEmpty(token.Id));
        Assert.Equal(SecurityAlgorithms.HmacSha256, token.Alg);
    }

    [Fact]
    public void Issue_ExpiresAfterConfiguredLifetime()
    {
        var accessToken = CreateIssuer().Issue("g1");

        var token = new JsonWebToken(accessToken.Token);
        var expectedExpiry = new DateTimeOffset(2026, 10, 7, 13, 0, 0, TimeSpan.Zero);
        Assert.Equal(expectedExpiry, accessToken.ExpiresAt);
        Assert.Equal(expectedExpiry.UtcDateTime, token.ValidTo);
        Assert.Equal(new DateTime(2026, 10, 7, 12, 0, 0, DateTimeKind.Utc), token.IssuedAt);
    }

    [Fact]
    public void Issue_UsesConfiguredLifetime()
    {
        _options.Lifetime = TimeSpan.FromMinutes(15);

        var accessToken = CreateIssuer().Issue("g1");

        Assert.Equal(new DateTimeOffset(2026, 10, 7, 12, 15, 0, TimeSpan.Zero), accessToken.ExpiresAt);
    }

    [Fact]
    public void Issue_GivesEveryTokenAUniqueId()
    {
        var issuer = CreateIssuer();

        Assert.NotEqual(new JsonWebToken(issuer.Issue("g1").Token).Id, new JsonWebToken(issuer.Issue("g1").Token).Id);
    }

    [Theory]
    [InlineData("")]
    [InlineData(" ")]
    public void Issue_WithBlankSubject_ThrowsArgumentException(string subjectId)
    {
        Assert.Throws<ArgumentException>(() => CreateIssuer().Issue(subjectId));
    }

    [Fact]
    public void Options_HaveDocumentedDefaults()
    {
        var options = new AccessTokenOptions();

        Assert.Equal("leak-out-server", options.Issuer);
        Assert.Equal("leak-out-game", options.Audience);
        Assert.Equal(TimeSpan.FromHours(1), options.Lifetime);
    }

    [Fact]
    public void Options_WithLongEnoughKey_AreValid()
    {
        Assert.True(Validate(new AccessTokenOptions { SigningKey = SigningKey }).Succeeded);
    }

    [Theory]
    [InlineData("")]
    [InlineData("too-short-key")]
    [InlineData("0123456789012345678901234567890")]
    public void Options_WithShortOrMissingKey_FailValidation(string signingKey)
    {
        var result = Validate(new AccessTokenOptions { SigningKey = signingKey });

        Assert.True(result.Failed);
        Assert.Contains("SigningKey", result.FailureMessage);
    }

    [Fact]
    public void Options_WithLifetimeOutOfRange_FailValidation()
    {
        Assert.True(Validate(new AccessTokenOptions { SigningKey = SigningKey, Lifetime = TimeSpan.FromDays(2) }).Failed);
    }

    private JwtAccessTokenIssuer CreateIssuer() => new(Options.Create(_options), _clock.Object);

    private static ValidateOptionsResult Validate(AccessTokenOptions options) =>
        new DataAnnotationValidateOptions<AccessTokenOptions>(null).Validate(null, options);
}
