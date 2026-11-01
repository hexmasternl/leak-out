using HexMaster.LeakOut.Core;

namespace HexMaster.LeakOut.DailyPipes.DomainModels;

/// <summary>
/// A player's run of consecutive UTC days with a daily pipe win. The stored <see cref="Current"/> is
/// the streak as of <see cref="LastWinDate"/>; <see cref="CurrentOn"/> reports it as 0 once a day was
/// missed, so no nightly sweep over all players is needed.
/// </summary>
public sealed class DailyStreak
{
    private DailyStreak()
    {
    }

    /// <summary>The internal id of the player; never the Google Play Games player id.</summary>
    public Guid UserId { get; private set; }

    /// <summary>The streak on <see cref="LastWinDate"/>.</summary>
    public int Current { get; private set; }

    /// <summary>The highest streak the player has ever reached.</summary>
    public int Best { get; private set; }

    /// <summary>The date of the player's last daily pipe win, or <see langword="null"/> before the first one.</summary>
    public DateOnly? LastWinDate { get; private set; }

    /// <summary>A player who has not won a daily pipe yet.</summary>
    public static DailyStreak Start(Guid userId)
    {
        if (userId == Guid.Empty)
        {
            throw new DomainException("A streak must belong to a player.");
        }

        return new DailyStreak { UserId = userId };
    }

    /// <summary>Restores a stored streak.</summary>
    public static DailyStreak Restore(Guid userId, int current, int best, DateOnly? lastWinDate)
    {
        var streak = Start(userId);
        if (current < 0 || best < current)
        {
            throw new DomainException("A streak cannot be negative or above the best streak.");
        }

        if ((current == 0) != (lastWinDate is null))
        {
            throw new DomainException("A streak has a last win date exactly when it is above 0.");
        }

        streak.Current = current;
        streak.Best = best;
        streak.LastWinDate = lastWinDate;
        return streak;
    }

    /// <summary>
    /// Counts a win on <paramref name="date"/>: the streak goes up by 1 when the last win was the day
    /// before, and restarts at 1 otherwise. A further win on the same date, or a win on an earlier
    /// date, changes nothing.
    /// </summary>
    /// <returns><see langword="true"/> when the streak changed.</returns>
    public bool RecordWin(DateOnly date)
    {
        if (LastWinDate is { } last && date <= last)
        {
            return false;
        }

        Current = LastWinDate == date.AddDays(-1) ? Current + 1 : 1;
        Best = Math.Max(Best, Current);
        LastWinDate = date;
        return true;
    }

    /// <summary>The streak as seen on <paramref name="today"/>: 0 when the last win is before yesterday.</summary>
    public int CurrentOn(DateOnly today) =>
        LastWinDate is { } last && last >= today.AddDays(-1) ? Current : 0;

    /// <summary>Whether the player has won the pipe of <paramref name="today"/>.</summary>
    public bool WonOn(DateOnly today) => LastWinDate == today;
}
