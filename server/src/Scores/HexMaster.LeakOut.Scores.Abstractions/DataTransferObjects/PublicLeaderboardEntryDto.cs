namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>
/// One ranked best score on a level for anonymous readers such as the website: its 1-based position,
/// the player's display name and the score. Carries no player id.
/// </summary>
public sealed record PublicLeaderboardEntryDto(int Position, string DisplayName, int Score);
