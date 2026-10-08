namespace HexMaster.LeakOut.Players.Abstractions.DataTransferObjects;

/// <summary>The body of a sign-in: the one-time server auth code the game got from Play Games.</summary>
public sealed record CreateSessionRequest(string? ServerAuthCode);
