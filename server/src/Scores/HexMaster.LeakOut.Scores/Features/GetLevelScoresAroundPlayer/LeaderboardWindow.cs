namespace HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;

/// <summary>
/// The part of a level's ranking shown around a player: the 1-based position of its first entry and
/// the number of entries.
/// </summary>
public readonly record struct LeaderboardWindow(int Start, int Count)
{
    public const int Size = 10;
    public const int EntriesAbove = 5;

    /// <summary>
    /// Centres the window on <paramref name="position"/> with <see cref="EntriesAbove"/> entries above
    /// it, shifting it down at the top of the ranking and up at the bottom, so it holds
    /// <see cref="Size"/> entries or all of them when the level has fewer.
    /// </summary>
    public static LeaderboardWindow Compute(int position, int total)
    {
        ArgumentOutOfRangeException.ThrowIfLessThan(position, 1);
        ArgumentOutOfRangeException.ThrowIfLessThan(total, position);

        var start = Math.Max(1, Math.Min(position - EntriesAbove, total - Size + 1));
        return new LeaderboardWindow(start, Math.Min(Size, total));
    }
}
