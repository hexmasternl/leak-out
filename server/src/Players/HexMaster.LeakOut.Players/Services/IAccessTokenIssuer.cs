namespace HexMaster.LeakOut.Players.Services;

/// <summary>Issues the server's own access tokens.</summary>
public interface IAccessTokenIssuer
{
    /// <summary>Issues an access token whose <c>sub</c> claim is the given subject id.</summary>
    AccessToken Issue(string subjectId);
}

/// <summary>A signed access token and the moment it expires.</summary>
public sealed record AccessToken(string Token, DateTimeOffset ExpiresAt);
