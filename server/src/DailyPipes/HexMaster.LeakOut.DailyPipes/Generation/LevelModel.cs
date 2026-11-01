namespace HexMaster.LeakOut.DailyPipes.Generation;

// A C# port of the app's level model (app/src/core/.../level/*.kt). The level JSON format
// (docs/level-format.md) is the contract between the two; keep both in step. Enum member order
// matches the Kotlin entries, because the solver's search order depends on it.

/// <summary>A side of a grid cell, with <c>x</c> growing to the right and <c>y</c> growing downward.</summary>
public enum Side
{
    Left,
    Right,
    Top,
    Bottom,
}

/// <summary>The pipe sections that can appear on the stack, by the ids used in level files.</summary>
public enum PipeSection
{
    H,
    V,
    LT,
    LB,
    RT,
    RB,
    X,
    RES,
}

public static class SideExtensions
{
    /// <summary>The sides in the order of the Kotlin <c>Side.entries</c>.</summary>
    public static IReadOnlyList<Side> All { get; } = [Side.Left, Side.Right, Side.Top, Side.Bottom];

    public static Side Opposite(this Side side) => side switch
    {
        Side.Left => Side.Right,
        Side.Right => Side.Left,
        Side.Top => Side.Bottom,
        _ => Side.Top,
    };

    public static int Dx(this Side side) => side switch
    {
        Side.Left => -1,
        Side.Right => 1,
        _ => 0,
    };

    public static int Dy(this Side side) => side switch
    {
        Side.Top => -1,
        Side.Bottom => 1,
        _ => 0,
    };

    /// <summary>The name used in level files: <c>LEFT</c>, <c>RIGHT</c>, <c>TOP</c> or <c>BOTTOM</c>.</summary>
    public static string ToJsonName(this Side side) => side.ToString().ToUpperInvariant();
}

public static class PipeSectionExtensions
{
    /// <summary>The sections in the order of the Kotlin <c>PipeSection.entries</c>.</summary>
    public static IReadOnlyList<PipeSection> All { get; } =
        [PipeSection.H, PipeSection.V, PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB, PipeSection.X, PipeSection.RES];

    /// <summary>Straight and corner sections: every level must be able to draw them.</summary>
    public static bool IsAlwaysOnStack(this PipeSection section) => section is not (PipeSection.X or PipeSection.RES);
}

/// <summary>A cell on the grid: <see cref="X"/> is the zero-based column, <see cref="Y"/> the zero-based row from the top.</summary>
public readonly record struct Position(int X, int Y)
{
    public Position Neighbour(Side side) => new(X + side.Dx(), Y + side.Dy());

    public override string ToString() => $"({X}, {Y})";
}

/// <summary>The size of a level's grid in cells.</summary>
public sealed record GridSize(int Columns, int Rows)
{
    public bool Contains(Position position) =>
        position.X >= 0 && position.X < Columns && position.Y >= 0 && position.Y < Rows;

    public override string ToString() => $"{Columns} by {Rows}";
}

/// <summary>The start, the finish or a portal: a cell and the side of it with the opening.</summary>
public sealed record Endpoint(Position Position, Side OpenSide)
{
    /// <summary>The neighbouring cell on the open side, which a pipe must occupy to connect this endpoint.</summary>
    public Position FacingCell => Position.Neighbour(OpenSide);
}

/// <summary>How the liquid moves. Speeds are in cell lengths per second.</summary>
public sealed record FlowSettings(
    long InitialDelayMs,
    double CellsPerSecond,
    double ReservoirSpeedFactor = LevelDefaults.ReservoirSpeedFactor,
    double SpeedRampPerCell = LevelDefaults.SpeedRampPerCell);

/// <summary>The upcoming-sections queue: how many are shown, and how often each section is drawn.</summary>
public sealed record StackSettings(int VisibleSize, IReadOnlyDictionary<PipeSection, int> Weights)
{
    public static StackSettings Default { get; } = new(LevelDefaults.VisibleStackSize, LevelDefaults.StackWeights);

    public int WeightOf(PipeSection section) => Weights[section];
}

/// <summary>Whether an unfilled pipe may be replaced, and how long placing is locked after a replacement.</summary>
public sealed record PlacementRules(bool AllowReplace = LevelDefaults.AllowReplace, long ReplacePenaltyMs = LevelDefaults.ReplacePenaltyMs);

/// <summary>The minimum points for one, two and three stars on a won level.</summary>
public sealed record StarBenchmarks(int One, int Two, int Three);

/// <summary>A section on the board from the start of every attempt, which the liquid must run through.</summary>
public sealed record MandatorySection(Position Position, PipeSection Section);

/// <summary>Two portals that link two cells of the board, each with the shape of an <see cref="Endpoint"/>.</summary>
public sealed record PortalPair(Endpoint A, Endpoint B)
{
    public Endpoint? PortalAt(Position position) =>
        position == A.Position ? A : position == B.Position ? B : null;

    public Endpoint? PartnerOf(Position position) =>
        position == A.Position ? B : position == B.Position ? A : null;
}

/// <summary>Engine settings used when a level file leaves them out, as in the app's <c>LevelDefaults</c>.</summary>
public static class LevelDefaults
{
    public const double ReservoirSpeedFactor = 0.3;
    public const double SpeedRampPerCell = 0.0;
    public const int VisibleStackSize = 5;
    public const bool AllowReplace = true;
    public const long ReplacePenaltyMs = 1_500;

    public static IReadOnlyDictionary<PipeSection, int> StackWeights { get; } = new Dictionary<PipeSection, int>
    {
        [PipeSection.H] = 7,
        [PipeSection.V] = 7,
        [PipeSection.LT] = 7,
        [PipeSection.LB] = 7,
        [PipeSection.RT] = 7,
        [PipeSection.RB] = 7,
        [PipeSection.X] = 0,
        [PipeSection.RES] = 0,
    };
}

/// <summary>
/// One playable level with concrete values only. <see cref="Blocked"/>, <see cref="Mandatory"/>,
/// <see cref="Portals"/> and <see cref="Leaky"/> keep their entries in authored order.
/// </summary>
public sealed record Level(
    int Id,
    string Name,
    GridSize Grid,
    Endpoint Start,
    Endpoint Finish,
    IReadOnlyList<Position> Blocked,
    FlowSettings Flow,
    StackSettings Stack,
    PlacementRules Rules,
    StarBenchmarks Stars,
    IReadOnlyList<MandatorySection> Mandatory,
    IReadOnlyList<PortalPair> Portals,
    IReadOnlyList<Position> Leaky)
{
    public bool IsBlocked(Position position) => Blocked.Contains(position);

    public bool IsLeaky(Position position) => Leaky.Contains(position);

    public MandatorySection? MandatoryAt(Position position) => Mandatory.FirstOrDefault(m => m.Position == position);

    public Endpoint? PortalAt(Position position) => Portals.Select(p => p.PortalAt(position)).FirstOrDefault(p => p is not null);

    public Endpoint? PartnerOf(Position position) => Portals.Select(p => p.PartnerOf(position)).FirstOrDefault(p => p is not null);

    /// <summary>The cells of every portal, pair by pair.</summary>
    public IEnumerable<Position> PortalCells => Portals.SelectMany(p => new[] { p.A.Position, p.B.Position });

    /// <summary>The cells a pipe can go on: every cell except blocked cells, the start, the finish and portals.</summary>
    public int FreeCells
    {
        get
        {
            var taken = Blocked.Append(Start.Position).Append(Finish.Position).Concat(PortalCells)
                .Where(Grid.Contains)
                .ToHashSet();
            return (Grid.Columns * Grid.Rows) - taken.Count;
        }
    }

    /// <summary>The free cells on which the liquid can earn points: <see cref="FreeCells"/> without the leaky tiles.</summary>
    public int ScoringCells
    {
        get
        {
            var taken = Blocked.Append(Start.Position).Append(Finish.Position).Concat(PortalCells).ToHashSet();
            return FreeCells - Leaky.Where(c => Grid.Contains(c) && !taken.Contains(c)).Distinct().Count();
        }
    }

    /// <summary>A cell can carry a pipe when it is in the grid, unblocked, and not the start, the finish or a portal.</summary>
    public bool IsFree(Position cell) =>
        Grid.Contains(cell) && !IsBlocked(cell) && cell != Start.Position && cell != Finish.Position && PortalAt(cell) is null;
}
