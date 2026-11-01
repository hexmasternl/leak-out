namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>A player's best result on a daily pipe.</summary>
public sealed record DailyBestScoreDto(int Score, int Stars);
