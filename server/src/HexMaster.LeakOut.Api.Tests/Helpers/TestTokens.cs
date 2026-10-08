using System.Text;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace HexMaster.LeakOut.Api.Tests.Helpers;

/// <summary>Creates access tokens shaped like the server's own, with overridable parts.</summary>
public static class TestTokens
{
    public const string SubjectId = "g1234567890";

    public static string Create(
        string? subjectId = SubjectId,
        string signingKey = LeakOutApiFactory.SigningKey,
        string issuer = "leak-out-server",
        string audience = "leak-out-game",
        DateTimeOffset? expiresAt = null)
    {
        var expires = expiresAt ?? DateTimeOffset.UtcNow.AddHours(1);
        var claims = new Dictionary<string, object> { [JwtRegisteredClaimNames.Jti] = Guid.NewGuid().ToString("N") };
        if (subjectId is not null)
        {
            claims[JwtRegisteredClaimNames.Sub] = subjectId;
        }

        return new JsonWebTokenHandler().CreateToken(new SecurityTokenDescriptor
        {
            Issuer = issuer,
            Audience = audience,
            IssuedAt = expires.AddHours(-1).UtcDateTime,
            NotBefore = expires.AddHours(-1).UtcDateTime,
            Expires = expires.UtcDateTime,
            Claims = claims,
            SigningCredentials = new SigningCredentials(
                new SymmetricSecurityKey(Encoding.UTF8.GetBytes(signingKey)),
                SecurityAlgorithms.HmacSha256),
        });
    }
}
