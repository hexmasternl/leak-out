namespace HexMaster.LeakOut.Players.Features.SignInPlayer;

/// <summary>Signs a player in with the one-time server auth code the game got from Play Games.</summary>
public sealed record SignInPlayerCommand(string ServerAuthCode);
