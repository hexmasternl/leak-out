namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>
/// A player's daily pipe streak: the consecutive UTC days with a win up to today (0 after a missed
/// day), the best streak ever, and whether today's pipe has been won.
/// </summary>
public sealed record DailyStreakDto(int Current, int Best, bool WonToday);
