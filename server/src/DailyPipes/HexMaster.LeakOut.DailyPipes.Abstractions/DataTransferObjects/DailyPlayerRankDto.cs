namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>The requesting player's own position and best result on a daily pipe.</summary>
public sealed record DailyPlayerRankDto(int Position, int Score, int Stars);
