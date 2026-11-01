using System.Collections.Immutable;

namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>The outcome of running <see cref="LevelSolver"/> on a level.</summary>
public abstract record SolverResult
{
    private SolverResult()
    {
    }

    /// <summary>
    /// A route exists. <see cref="Path"/> runs from the start's facing cell to the finish's facing cell,
    /// inclusive. It includes the mandatory sections (a mandatory cross twice, once per channel) and,
    /// for a pass through a portal pair, both portals in flow order. <see cref="LeakFree"/> tells
    /// whether some route avoids every leaky tile.
    /// </summary>
    public sealed record Solvable(IReadOnlyList<Position> Path, bool LeakFree = true) : SolverResult;

    /// <summary>No route was found. <see cref="Reason"/> names what could not be connected.</summary>
    public sealed record Unsolvable(string Reason) : SolverResult;
}

/// <summary>
/// A C# port of the app's <c>LevelSolver</c>: decides whether pipe sections can connect the start's
/// facing cell to the finish's facing cell over free cells, running through every channel of every
/// mandatory section, possibly through portal pairs. The search, its order and its work limit are
/// the Kotlin original's, so both reach the same verdict; the shared conformance fixtures check it.
/// </summary>
public static class LevelSolver
{
    /// <summary>The most cells the search may visit before the solver gives up.</summary>
    public const long MaxSearchWork = 10_000_000;

    public static SolverResult Solve(Level level)
    {
        ArgumentNullException.ThrowIfNull(level);

        var result = Route(level);
        if (result is not SolverResult.Solvable solvable || level.Leaky.Count == 0)
        {
            return result;
        }

        var withoutLeaky = level with { Blocked = [.. level.Blocked, .. level.Leaky], Leaky = [] };
        return solvable with { LeakFree = Route(withoutLeaky) is SolverResult.Solvable };
    }

    private static SolverResult Route(Level level) =>
        UnreachableEndpoint(level, level.Start.FacingCell, level.Finish.FacingCell)
        ?? UnpassableMandatory(level)
        ?? new Search(level).Run();

    private static SolverResult.Unsolvable? UnreachableEndpoint(Level level, Position from, Position to)
    {
        foreach (var (name, cell) in new[] { ("start", from), ("finish", to) })
        {
            if (!level.IsFree(cell))
            {
                var why = level.Grid.Contains(cell) ? "is blocked or holds an endpoint or a portal" : $"lies outside the {level.Grid} grid";
                return new SolverResult.Unsolvable($"The {name}'s facing cell {cell} {why}, so the {name} can never be connected");
            }
        }

        return null;
    }

    private static SolverResult.Unsolvable? UnpassableMandatory(Level level)
    {
        for (var index = 0; index < level.Mandatory.Count; index++)
        {
            var mandatory = level.Mandatory[index];
            foreach (var axis in AxesOf(mandatory.Section))
            {
                Side? stuck = axis.Where(side => !CanAdjoin(level, mandatory.Position, side)).Cast<Side?>().FirstOrDefault();
                if (stuck is { } side)
                {
                    return new SolverResult.Unsolvable(
                        $"The {Describe(mandatory, index)} can never be passed {AxisName(axis)}: its neighbour " +
                        $"{mandatory.Position.Neighbour(side)} on the {side.ToString().ToLowerInvariant()} cannot carry a pipe");
                }
            }
        }

        return null;
    }

    private static bool CanAdjoin(Level level, Position cell, Side side)
    {
        var neighbour = cell.Neighbour(side);
        if (neighbour == level.Start.Position)
        {
            return level.Start.FacingCell == cell;
        }

        if (neighbour == level.Finish.Position)
        {
            return level.Finish.FacingCell == cell;
        }

        var portal = level.PortalAt(neighbour);
        return portal is not null ? portal.FacingCell == cell : level.IsFree(neighbour);
    }

    private static readonly Side[] Horizontal = [Side.Left, Side.Right];
    private static readonly Side[] Vertical = [Side.Top, Side.Bottom];

    private static IReadOnlyList<Side[]> AxesOf(PipeSection section) =>
        section == PipeSection.X ? [Horizontal, Vertical] : [Horizontal];

    private static string AxisName(Side[] axis) => axis.Contains(Side.Left) ? "from left to right" : "from top to bottom";

    private static string Describe(MandatorySection mandatory, int index) =>
        $"mandatory {mandatory.Section} at {mandatory.Position} (mandatory[{index}])";

    private sealed record Waypoint(int Index, Position Cell, Side[] Axis);

    private sealed record Jump(Position EntryPortal, Position ExitPortal, Position To);

    private sealed class Reach(Dictionary<Position, Position> cameFrom, Dictionary<Position, Jump> jumps)
    {
        public static Reach None { get; } = new([], []);

        public int Size => cameFrom.Count;

        public bool Contains(Position cell) => cameFrom.ContainsKey(cell);

        public List<Position> PathTo(Position to)
        {
            var path = new List<Position> { to };
            var cell = to;
            while (cameFrom[cell] != cell)
            {
                if (jumps.TryGetValue(cell, out var jump))
                {
                    path.Add(jump.ExitPortal);
                    path.Add(jump.EntryPortal);
                }

                cell = cameFrom[cell];
                path.Add(cell);
            }

            path.Reverse();
            return path;
        }
    }

    private sealed class Search
    {
        private readonly Level _level;
        private readonly List<Waypoint> _waypoints;
        private readonly HashSet<Position> _mandatoryCells;
        private readonly Dictionary<Position, List<Jump>> _jumpsFrom = [];
        private long _work;
        private int _deepest = -1;
        private Waypoint? _stuckAt;

        public Search(Level level)
        {
            _level = level;
            _waypoints = level.Mandatory
                .SelectMany((m, index) => AxesOf(m.Section).Select(axis => new Waypoint(index, m.Position, axis)))
                .ToList();
            _mandatoryCells = level.Mandatory.Select(m => m.Position).ToHashSet();
            foreach (var pair in level.Portals)
            {
                foreach (var (entry, exit) in new[] { (pair.A, pair.B), (pair.B, pair.A) })
                {
                    if (!_jumpsFrom.TryGetValue(entry.FacingCell, out var list))
                    {
                        list = [];
                        _jumpsFrom[entry.FacingCell] = list;
                    }

                    list.Add(new Jump(entry.Position, exit.Position, exit.FacingCell));
                }
            }
        }

        public SolverResult Run()
        {
            var path = Extend(_level.Start.Position, _level.Start.FacingCell, [], []);
            if (path is not null)
            {
                return new SolverResult.Solvable(path);
            }

            var from = _level.Start.FacingCell;
            var to = _level.Finish.FacingCell;
            if (_waypoints.Count == 0)
            {
                return new SolverResult.Unsolvable(
                    $"The finish's facing cell {to} cannot be reached from the start's facing cell {from}: " +
                    "blocked cells or the grid's edge seal it off");
            }

            if (_stuckAt is null)
            {
                return new SolverResult.Unsolvable(
                    $"The finish's facing cell {to} cannot be reached after running through every mandatory section");
            }

            return new SolverResult.Unsolvable(
                $"No route from the start's facing cell {from} runs through the " +
                $"{Describe(_level.Mandatory[_stuckAt.Index], _stuckAt.Index)} {AxisName(_stuckAt.Axis)}");
        }

        private List<Position>? Extend(Position previous, Position next, ImmutableHashSet<Position> used, ImmutableHashSet<Waypoint> passed)
        {
            if (_work > MaxSearchWork)
            {
                return null;
            }

            var portal = _level.PortalAt(next);
            if (portal is not null)
            {
                var exit = _level.PartnerOf(next);
                if (exit is null || portal.FacingCell != previous || used.Contains(next))
                {
                    return null;
                }

                var after = Extend(exit.Position, exit.FacingCell, used.Add(next).Add(exit.Position), passed);
                return after is null ? null : [next, exit.Position, .. after];
            }

            var reach = IsOpen(next, used) ? ShortestPaths(next, used) : Reach.None;
            _work += reach.Size + used.Count + 1;

            if (passed.Count == _waypoints.Count)
            {
                var last = Segment(previous, next, _level.Finish.FacingCell, _level.Finish.Position, reach, used);
                if (last is null)
                {
                    NoteStuck(passed.Count, null);
                }

                return last;
            }

            var candidates = new List<(Waypoint Waypoint, Side Direction, List<Position> Approach)>();
            foreach (var waypoint in _waypoints.Where(w => !passed.Contains(w)))
            {
                foreach (var direction in waypoint.Axis)
                {
                    var entry = waypoint.Cell.Neighbour(direction.Opposite());
                    var approach = Segment(previous, next, entry, waypoint.Cell, reach, used);
                    if (approach is not null)
                    {
                        candidates.Add((waypoint, direction, approach));
                    }
                }
            }

            if (candidates.Count == 0)
            {
                NoteStuck(passed.Count, _waypoints.First(w => !passed.Contains(w)));
            }

            foreach (var (waypoint, direction, approach) in candidates.OrderBy(c => c.Approach.Count))
            {
                var rest = Extend(waypoint.Cell, waypoint.Cell.Neighbour(direction), used.Union(approach), passed.Add(waypoint));
                if (rest is null)
                {
                    continue;
                }

                return [.. approach, waypoint.Cell, .. rest];
            }

            return null;
        }

        private List<Position>? Segment(Position previous, Position next, Position entry, Position target, Reach reach, ImmutableHashSet<Position> used)
        {
            if (next == target)
            {
                return previous == entry ? [] : null;
            }

            if (reach.Contains(entry))
            {
                return reach.PathTo(entry);
            }

            var portal = _level.PortalAt(entry);
            var other = _level.PartnerOf(entry);
            if (portal is null || other is null)
            {
                return null;
            }

            if (portal.FacingCell != target || used.Contains(entry) || !reach.Contains(other.FacingCell))
            {
                return null;
            }

            return [.. reach.PathTo(other.FacingCell), other.Position, entry];
        }

        private void NoteStuck(int depth, Waypoint? waypoint)
        {
            if (depth > _deepest)
            {
                _deepest = depth;
                _stuckAt = waypoint;
            }
        }

        private bool IsOpen(Position cell, ImmutableHashSet<Position> used) =>
            _level.IsFree(cell) && !_mandatoryCells.Contains(cell) && !used.Contains(cell);

        private Reach ShortestPaths(Position from, ImmutableHashSet<Position> used)
        {
            var cameFrom = new Dictionary<Position, Position> { [from] = from };
            var jumps = new Dictionary<Position, Jump>();
            var queue = new Queue<Position>();
            queue.Enqueue(from);
            while (queue.Count > 0)
            {
                var cell = queue.Dequeue();
                foreach (var side in SideExtensions.All)
                {
                    var neighbour = cell.Neighbour(side);
                    if (!IsOpen(neighbour, used) || cameFrom.ContainsKey(neighbour))
                    {
                        continue;
                    }

                    cameFrom[neighbour] = cell;
                    queue.Enqueue(neighbour);
                }

                if (_jumpsFrom.TryGetValue(cell, out var cellJumps))
                {
                    foreach (var jump in cellJumps)
                    {
                        if (!IsOpen(jump.To, used) || cameFrom.ContainsKey(jump.To) || used.Contains(jump.EntryPortal))
                        {
                            continue;
                        }

                        cameFrom[jump.To] = cell;
                        jumps[jump.To] = jump;
                        queue.Enqueue(jump.To);
                    }
                }
            }

            return new Reach(cameFrom, jumps);
        }
    }
}
