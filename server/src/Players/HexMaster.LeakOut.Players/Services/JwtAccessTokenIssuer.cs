using HexMaster.LeakOut.Core;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>Issues HS256-signed JWTs whose <c>sub</c> claim is the Google Play Games player id.</summary>
public sealed class JwtAccessTokenIssuer(IOptions<AccessTokenOptions> options, IClock clock) : IAccessTokenIssuer
{
    private readonly JsonWebTokenHandler _tokenHandler = new();

    public AccessToken Issue(string subjectId)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(subjectId);

        var settings = options.Value;

        // JWT timestamps have second precision; truncate so the reported expiry matches the token.
        var issuedAt = DateTimeOffset.FromUnixTimeSeconds(clock.UtcNow.ToUnixTimeSeconds());
        var expiresAt = issuedAt.Add(settings.Lifetime);

        var descriptor = new SecurityTokenDescriptor
        {
            Issuer = settings.Issuer,
            Audience = settings.Audience,
            IssuedAt = issuedAt.UtcDateTime,
            NotBefore = issuedAt.UtcDateTime,
            Expires = expiresAt.UtcDateTime,
            Claims = new Dictionary<string, object>
            {
                [JwtRegisteredClaimNames.Sub] = subjectId,
                [JwtRegisteredClaimNames.Jti] = Guid.NewGuid().ToString("N"),
            },
            SigningCredentials = new SigningCredentials(settings.CreateSigningKey(), SecurityAlgorithms.HmacSha256),
        };

        return new AccessToken(_tokenHandler.CreateToken(descriptor), expiresAt);
    }
}
