namespace HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;

/// <summary>A signed-in session: the access token for later requests and the player it belongs to.</summary>
public sealed record SessionDto(string AccessToken, DateTimeOffset ExpiresAt, Guid PlayerId, string DisplayName);
