namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>
/// One ranked best score on a level for anonymous readers such as the website: its 1-based position,
/// the player's display name, the score and the moment the server received that score. Carries no
/// player id.
/// </summary>
public sealed record PublicLeaderboardEntryDto(int Position, string DisplayName, int Score, DateTimeOffset AchievedAt);
