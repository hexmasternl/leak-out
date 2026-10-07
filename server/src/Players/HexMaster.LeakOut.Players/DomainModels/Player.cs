using HexMaster.LeakOut.Core;

namespace HexMaster.LeakOut.Players.DomainModels;

/// <summary>
/// A player of the Leak Out game. The player is identified by an internal id only; the
/// Google Play Games player id it was registered with is kept by the data layer.
/// </summary>
public sealed class Player
{
    private Player()
    {
        DisplayName = string.Empty;
    }

    public Guid Id { get; private set; }

    /// <summary>The display name Google reported at the last sign-in; empty when Google reported none.</summary>
    public string DisplayName { get; private set; }

    public DateTimeOffset CreatedAt { get; private set; }

    public DateTimeOffset LastSignedInAt { get; private set; }

    /// <summary>Registers a player signing in for the first time.</summary>
    public static Player Register(string? displayName, DateTimeOffset now) => new()
    {
        Id = Guid.CreateVersion7(now),
        DisplayName = NormalizeDisplayName(displayName),
        CreatedAt = now,
        LastSignedInAt = now,
    };

    /// <summary>Rebuilds a player from stored state.</summary>
    public static Player Restore(Guid id, string displayName, DateTimeOffset createdAt, DateTimeOffset lastSignedInAt)
    {
        if (id == Guid.Empty)
        {
            throw new DomainException("A player must have an id.");
        }

        if (lastSignedInAt < createdAt)
        {
            throw new DomainException("A player cannot have signed in before it was registered.");
        }

        return new Player
        {
            Id = id,
            DisplayName = NormalizeDisplayName(displayName),
            CreatedAt = createdAt,
            LastSignedInAt = lastSignedInAt,
        };
    }

    /// <summary>Records a later sign-in and refreshes the display name to the one Google reports now.</summary>
    public void RecordSignIn(string? displayName, DateTimeOffset now)
    {
        if (now < CreatedAt)
        {
            throw new DomainException("A player cannot sign in before it was registered.");
        }

        DisplayName = NormalizeDisplayName(displayName);
        LastSignedInAt = now;
    }

    private static string NormalizeDisplayName(string? displayName) =>
        string.IsNullOrWhiteSpace(displayName) ? string.Empty : displayName.Trim();
}
