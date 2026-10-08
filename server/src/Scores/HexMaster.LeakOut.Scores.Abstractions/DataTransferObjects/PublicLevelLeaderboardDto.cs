namespace HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

/// <summary>The public top of a level's ranking: the level and its entries in ranking order.</summary>
public sealed record PublicLevelLeaderboardDto(int Level, IReadOnlyList<PublicLeaderboardEntryDto> Entries);
