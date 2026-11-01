namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>One ranked best result on a daily pipe, and whether it belongs to the requesting player.</summary>
public sealed record DailyLeaderboardEntryDto(int Position, string DisplayName, int Score, int Stars, bool IsCurrentPlayer);
