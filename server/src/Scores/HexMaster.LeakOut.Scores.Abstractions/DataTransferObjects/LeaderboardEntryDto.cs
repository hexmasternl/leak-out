namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>
/// One ranked best score on a level: its 1-based position, the player's display name, the score,
/// and whether it belongs to the requesting player.
/// </summary>
public sealed record LeaderboardEntryDto(int Position, string DisplayName, int Score, bool IsCurrentPlayer);
