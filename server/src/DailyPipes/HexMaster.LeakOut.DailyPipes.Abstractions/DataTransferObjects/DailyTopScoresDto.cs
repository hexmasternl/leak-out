namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>
/// The top of a daily pipe's ranking, and the requesting player's own rank, or <see langword="null"/>
/// when the player has no result on it.
/// </summary>
public sealed record DailyTopScoresDto(DateOnly Date, IReadOnlyList<DailyLeaderboardEntryDto> Entries, DailyPlayerRankDto? Player);
