namespace HexMaster.LeakOut.Core;

/// <summary>Abstracts the current time so time-dependent logic can be tested.</summary>
public interface IClock
{
    DateTimeOffset UtcNow { get; }
}
