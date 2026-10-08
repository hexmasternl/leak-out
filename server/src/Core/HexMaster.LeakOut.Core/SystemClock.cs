namespace HexMaster.LeakOut.Core;

/// <summary>The real clock, backed by <see cref="TimeProvider.System"/>.</summary>
public sealed class SystemClock : IClock
{
    public DateTimeOffset UtcNow => TimeProvider.System.GetUtcNow();
}
