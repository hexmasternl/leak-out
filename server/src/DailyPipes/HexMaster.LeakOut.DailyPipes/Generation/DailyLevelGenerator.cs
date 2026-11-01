namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>
/// A random candidate for a daily pipe: a level layout, the seed of the app's stack, and the head
/// start per cell of the route that <see cref="DailyPipeBuilder"/> adds to the level's base delay
/// once the solver has found the route.
/// </summary>
public sealed record DailyPipeCandidate(Level Level, long StackSeed, long DelayPerRouteCellMs);

/// <summary>Makes random daily pipe candidates. A candidate is not checked yet; it may be invalid or unsolvable.</summary>
public interface IDailyLevelGenerator
{
    DailyPipeCandidate Generate(DateOnly date);
}

/// <summary>
/// Generates daily pipe candidates within the level-format limits, sized for a phone, using the
/// <see cref="DifficultyPreset"/> of the date's weekday. Every candidate draws from a new random
/// source seeded by <c>nextSeed</c>, so a fixed seed sequence makes the generator reproducible.
/// </summary>
public sealed class DailyLevelGenerator(Func<int> nextSeed) : IDailyLevelGenerator
{
    /// <summary>The largest stack seed: 2^53, so the seed survives any JSON reader as an exact number.</summary>
    public const long MaxStackSeed = 1L << 53;

    /// <summary>A generator seeded from <see cref="Random.Shared"/>.</summary>
    public DailyLevelGenerator()
        : this(() => Random.Shared.Next())
    {
    }

    public DailyPipeCandidate Generate(DateOnly date)
    {
        var random = new Random(nextSeed());
        var preset = DifficultyPreset.For(date);
        var grid = new GridSize(
            random.Next(preset.MinColumns, preset.MaxColumns + 1),
            random.Next(preset.MinRows, preset.MaxRows + 1));

        // The start and finish sit on different edges, mostly on opposite ones.
        var startEdge = (Side)random.Next(4);
        var otherEdges = SideExtensions.All.Where(s => s != startEdge).ToList();
        var finishEdge = random.NextDouble() < 0.7 ? startEdge.Opposite() : otherEdges[random.Next(otherEdges.Count)];
        var start = EdgeEndpoint(grid, startEdge, random);
        var finish = EdgeEndpoint(grid, finishEdge, random);

        // Cells the layout keeps clear, so the start and finish are never walled off.
        var reserved = new HashSet<Position> { start.Position, start.FacingCell, finish.Position, finish.FacingCell };
        var free = AllCells(grid).Where(c => !reserved.Contains(c)).ToList();
        Shuffle(free, random);
        var next = 0;

        var blockedCount = random.Next(0, (int)(grid.Columns * grid.Rows * preset.MaxBlockedFraction) + 1);
        var blocked = free.Skip(next).Take(blockedCount).ToList();
        next += blocked.Count;

        var mandatory = new List<MandatorySection>();
        var mandatoryCount = random.Next(0, preset.MaxMandatory + 1);
        for (var i = 0; i < mandatoryCount && next < free.Count; i++)
        {
            mandatory.Add(new MandatorySection(free[next++], random.Next(2) == 0 ? PipeSection.X : PipeSection.RES));
        }

        var portals = new List<PortalPair>();
        if (random.NextDouble() < preset.PortalChance && next + 1 < free.Count)
        {
            portals.Add(new PortalPair(
                new Endpoint(free[next++], (Side)random.Next(4)),
                new Endpoint(free[next++], (Side)random.Next(4))));
        }

        var weights = LevelDefaults.StackWeights.ToDictionary(w => w.Key, w => w.Value);
        weights[PipeSection.X] = preset.CrossWeight;

        var level = new Level(
            Id: (date.Year * 10_000) + (date.Month * 100) + date.Day,
            Name: $"Daily Pipe {date:yyyy-MM-dd}",
            Grid: grid,
            Start: start,
            Finish: finish,
            Blocked: blocked,
            Flow: new FlowSettings(preset.BaseDelayMs, preset.CellsPerSecond),
            Stack: new StackSettings(LevelDefaults.VisibleStackSize, weights),
            Rules: new PlacementRules(),
            // Placeholder thresholds; DailyPipeBuilder derives the real ones from the solver's route.
            Stars: new StarBenchmarks(1, 2, 3),
            Mandatory: mandatory,
            Portals: portals,
            Leaky: []);

        return new DailyPipeCandidate(level, random.NextInt64(0, MaxStackSeed), preset.DelayPerRouteCellMs);
    }

    /// <summary>A cell on the edge of <paramref name="side"/> with its open side facing into the grid.</summary>
    private static Endpoint EdgeEndpoint(GridSize grid, Side side, Random random) => side switch
    {
        Side.Left => new Endpoint(new Position(0, random.Next(grid.Rows)), Side.Right),
        Side.Right => new Endpoint(new Position(grid.Columns - 1, random.Next(grid.Rows)), Side.Left),
        Side.Top => new Endpoint(new Position(random.Next(grid.Columns), 0), Side.Bottom),
        _ => new Endpoint(new Position(random.Next(grid.Columns), grid.Rows - 1), Side.Top),
    };

    private static IEnumerable<Position> AllCells(GridSize grid) =>
        Enumerable.Range(0, grid.Rows).SelectMany(y => Enumerable.Range(0, grid.Columns).Select(x => new Position(x, y)));

    private static void Shuffle(List<Position> cells, Random random)
    {
        for (var i = cells.Count - 1; i > 0; i--)
        {
            var j = random.Next(i + 1);
            (cells[i], cells[j]) = (cells[j], cells[i]);
        }
    }
}
