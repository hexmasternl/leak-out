namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>A view on the ranking of a level: the level and its entries in ranking order.</summary>
public sealed record LevelLeaderboardDto(int Level, IReadOnlyList<LeaderboardEntryDto> Entries);
