namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>
/// How hard the daily pipe of a weekday is: the grid sizes, how crowded the grid gets, which features
/// appear, how much head start the player gets and how fast the liquid runs. Monday is the easiest,
/// Sunday the hardest. Tuning these changes the daily pipe without an app release.
/// </summary>
public sealed record DifficultyPreset(
    int MinColumns,
    int MaxColumns,
    int MinRows,
    int MaxRows,
    double MaxBlockedFraction,
    int MaxMandatory,
    double PortalChance,
    long BaseDelayMs,
    long DelayPerRouteCellMs,
    double CellsPerSecond,
    int CrossWeight)
{
    /// <summary>The preset of the weekday of <paramref name="date"/>.</summary>
    public static DifficultyPreset For(DateOnly date) => ForWeekday(((int)date.DayOfWeek + 6) % 7);

    /// <summary>The preset of weekday <paramref name="index"/>: 0 is Monday, 6 is Sunday.</summary>
    public static DifficultyPreset ForWeekday(int index)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(index);
        ArgumentOutOfRangeException.ThrowIfGreaterThan(index, 6);

        var t = index / 6.0;
        return new DifficultyPreset(
            MinColumns: 7,
            MaxColumns: 9 + (int)Math.Round(5 * t),
            MinRows: 5,
            MaxRows: 7 + (int)Math.Round(3 * t),
            MaxBlockedFraction: 0.05 + (0.10 * t),
            MaxMandatory: index switch { < 2 => 0, < 5 => 1, _ => 2 },
            PortalChance: index < 3 ? 0.0 : 0.5,
            BaseDelayMs: 10_000 - (long)Math.Round(4_000 * t),
            DelayPerRouteCellMs: 900 - (long)Math.Round(400 * t),
            CellsPerSecond: Math.Round(0.22 + (0.12 * t), 3),
            CrossWeight: index < 2 ? 0 : 2);
    }
}
