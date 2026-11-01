namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>A problem with the meaning of a level. <see cref="Field"/> is the dotted path of the value concerned.</summary>
public sealed record LevelValidationError(string Field, string Message);

/// <summary>
/// A C# port of the app's <c>LevelValidator</c>: checks that a level can be played (value ranges,
/// positions inside the grid, start and finish placed so that they can be connected) and reports every
/// problem. It does not check solvability; that is <see cref="LevelSolver"/>'s job. The shared
/// conformance fixtures keep this port and the Kotlin original in step.
/// </summary>
public static class LevelRules
{
    public const int MinGridSize = 4;
    public const int MaxGridColumns = 25;
    public const int MaxGridRows = 15;
    public const double MaxCellsPerSecond = 10.0;
    public const int MinVisibleStackSize = 1;
    public const int MaxVisibleStackSize = 10;
    public const int MaxMandatorySections = 6;
    public const int MaxPortalPairs = 3;

    public static IReadOnlyList<LevelValidationError> Validate(Level level)
    {
        ArgumentNullException.ThrowIfNull(level);

        var errors = new Errors();
        CheckIdentity(errors, level);
        CheckGrid(errors, level.Grid);
        CheckEndpoints(errors, level);
        CheckBlocked(errors, level);
        CheckMandatory(errors, level);
        CheckPortals(errors, level);
        CheckLeaky(errors, level);
        CheckFlow(errors, level.Flow);
        CheckStack(errors, level.Stack);
        CheckRules(errors, level.Rules);
        CheckStars(errors, level.Stars);
        return errors.List;
    }

    private static void CheckIdentity(Errors errors, Level level)
    {
        if (level.Id < 1)
        {
            errors.Add("id", $"id must be a positive number, but had {level.Id}");
        }

        if (string.IsNullOrWhiteSpace(level.Name))
        {
            errors.Add("name", "name must not be blank");
        }
    }

    private static void CheckGrid(Errors errors, GridSize grid)
    {
        if (grid.Columns is < MinGridSize or > MaxGridColumns)
        {
            errors.Add("grid.columns", $"grid.columns must be between {MinGridSize} and {MaxGridColumns}, but had {grid.Columns}");
        }

        if (grid.Rows is < MinGridSize or > MaxGridRows)
        {
            errors.Add("grid.rows", $"grid.rows must be between {MinGridSize} and {MaxGridRows}, but had {grid.Rows}");
        }
    }

    private static void CheckEndpoints(Errors errors, Level level)
    {
        var start = level.Start;
        var finish = level.Finish;
        var startInGrid = CheckInGrid(errors, "start", start, level.Grid);
        var finishInGrid = CheckInGrid(errors, "finish", finish, level.Grid);
        if (start.Position == finish.Position)
        {
            errors.Add("finish", $"Start and finish must be different cells, but both are at {start.Position}");
        }

        if (startInGrid)
        {
            CheckFacingCell(errors, "start", start, level);
        }

        if (finishInGrid)
        {
            CheckFacingCell(errors, "finish", finish, level);
        }

        var startFacesFinish = start.FacingCell == finish.Position;
        var finishFacesStart = finish.FacingCell == start.Position;
        if (startFacesFinish && finishFacesStart)
        {
            errors.Add("start", "Start and finish must not face each other directly: the level would be won without placing a pipe");
        }
        else if (startFacesFinish)
        {
            errors.Add("start.openSide", "The start's facing cell must not be the finish: the liquid would leak on the first tick");
        }
        else if (finishFacesStart)
        {
            errors.Add("finish.openSide", "The finish's facing cell must not be the start: the finish could never be reached");
        }
    }

    private static bool CheckInGrid(Errors errors, string name, Endpoint endpoint, GridSize grid)
    {
        var (x, y) = endpoint.Position;
        if (x < 0 || x >= grid.Columns)
        {
            errors.Add($"{name}.x", $"The {name}'s x {x} lies outside the grid's columns 0 to {grid.Columns - 1}");
        }

        if (y < 0 || y >= grid.Rows)
        {
            errors.Add($"{name}.y", $"The {name}'s y {y} lies outside the grid's rows 0 to {grid.Rows - 1}");
        }

        return grid.Contains(endpoint.Position);
    }

    private static void CheckFacingCell(Errors errors, string name, Endpoint endpoint, Level level)
    {
        var facing = endpoint.FacingCell;
        if (!level.Grid.Contains(facing))
        {
            errors.Add($"{name}.openSide", $"The {name}'s open side {endpoint.OpenSide.ToJsonName()} faces the edge of the grid, so it can never be connected");
        }
        else if (level.IsBlocked(facing))
        {
            errors.Add($"{name}.openSide", $"The {name} is walled off: its facing cell {facing} is blocked");
        }
        else if (level.PortalAt(facing) is not null)
        {
            errors.Add($"{name}.openSide", $"The {name}'s facing cell {facing} must not be a portal");
        }
    }

    private static void CheckPortals(Errors errors, Level level)
    {
        if (level.Portals.Count > MaxPortalPairs)
        {
            errors.Add("portals", $"A level can have at most {MaxPortalPairs} portal pairs, but had {level.Portals.Count}");
        }

        var firstField = new Dictionary<Position, string>();
        for (var index = 0; index < level.Portals.Count; index++)
        {
            var pair = level.Portals[index];
            foreach (var (name, portal) in new[] { ("a", pair.A), ("b", pair.B) })
            {
                var field = $"portals[{index}].{name}";
                var cell = portal.Position;
                if (!level.Grid.Contains(cell))
                {
                    errors.Add(field, $"Portal at {cell} lies outside the {level.Grid} grid");
                }
                else if (level.IsBlocked(cell))
                {
                    errors.Add(field, $"Portal at {cell} is on a blocked cell");
                }
                else if (cell == level.Start.Position || cell == level.Finish.Position)
                {
                    errors.Add(field, $"Portal at {cell} is on the start or finish; the start and finish cannot hold a portal");
                }
                else if (level.MandatoryAt(cell) is not null)
                {
                    errors.Add(field, $"Portal at {cell} is on a cell that holds a mandatory section");
                }
                else
                {
                    CheckPortalFacingCell(errors, field, portal, level);
                }

                if (!firstField.TryAdd(cell, field))
                {
                    errors.Add(field, $"Portal at {cell} duplicates {firstField[cell]}");
                }
            }
        }
    }

    private static void CheckPortalFacingCell(Errors errors, string field, Endpoint portal, Level level)
    {
        var facing = portal.FacingCell;
        if (!level.Grid.Contains(facing))
        {
            errors.Add($"{field}.openSide", $"The portal's open side {portal.OpenSide.ToJsonName()} faces the edge of the grid, so it can never be connected");
        }
        else if (level.IsBlocked(facing))
        {
            errors.Add($"{field}.openSide", $"The portal is walled off: its facing cell {facing} is blocked");
        }
        else if (facing == level.Start.Position || facing == level.Finish.Position || level.PortalAt(facing) is not null)
        {
            errors.Add($"{field}.openSide", $"A portal must not face the start, the finish or another portal, but its facing cell {facing} does");
        }
    }

    private static void CheckBlocked(Errors errors, Level level)
    {
        var firstIndex = new Dictionary<Position, int>();
        for (var index = 0; index < level.Blocked.Count; index++)
        {
            var cell = level.Blocked[index];
            var field = $"blocked[{index}]";
            if (!level.Grid.Contains(cell))
            {
                errors.Add(field, $"Blocked cell {cell} lies outside the {level.Grid} grid");
            }

            if (cell == level.Start.Position || cell == level.Finish.Position)
            {
                errors.Add(field, $"Blocked cell {cell} is on the start or finish; the start and finish cannot be blocked");
            }

            if (!firstIndex.TryAdd(cell, index))
            {
                errors.Add(field, $"Blocked cell {cell} duplicates blocked[{firstIndex[cell]}]");
            }
        }
    }

    private static void CheckMandatory(Errors errors, Level level)
    {
        if (level.Mandatory.Count > MaxMandatorySections)
        {
            errors.Add("mandatory", $"A level can have at most {MaxMandatorySections} mandatory sections, but had {level.Mandatory.Count}");
        }

        var firstIndex = new Dictionary<Position, int>();
        for (var index = 0; index < level.Mandatory.Count; index++)
        {
            var (cell, section) = level.Mandatory[index];
            var field = $"mandatory[{index}]";
            if (section is not (PipeSection.X or PipeSection.RES))
            {
                errors.Add($"{field}.section", $"Mandatory section {section} at {cell} is not allowed: only X and RES can be mandatory");
            }

            if (!level.Grid.Contains(cell))
            {
                errors.Add(field, $"Mandatory section at {cell} lies outside the {level.Grid} grid");
            }
            else if (level.IsBlocked(cell))
            {
                errors.Add(field, $"Mandatory section at {cell} is on a blocked cell");
            }
            else if (cell == level.Start.Position || cell == level.Finish.Position)
            {
                errors.Add(field, $"Mandatory section at {cell} is on the start or finish; the start and finish cannot hold a section");
            }

            if (!firstIndex.TryAdd(cell, index))
            {
                errors.Add(field, $"Mandatory section at {cell} duplicates mandatory[{firstIndex[cell]}]");
            }
        }
    }

    private static void CheckLeaky(Errors errors, Level level)
    {
        var firstIndex = new Dictionary<Position, int>();
        for (var index = 0; index < level.Leaky.Count; index++)
        {
            var cell = level.Leaky[index];
            var field = $"leaky[{index}]";
            if (!level.Grid.Contains(cell))
            {
                errors.Add(field, $"Leaky tile {cell} lies outside the {level.Grid} grid");
            }
            else if (level.IsBlocked(cell))
            {
                errors.Add(field, $"Leaky tile {cell} is on a blocked cell");
            }
            else if (cell == level.Start.Position || cell == level.Finish.Position)
            {
                errors.Add(field, $"Leaky tile {cell} is on the start or finish; the start and finish cannot be leaky");
            }
            else if (level.MandatoryAt(cell) is not null)
            {
                errors.Add(field, $"Leaky tile {cell} holds a mandatory section; a mandatory section cannot lie on a leaky tile");
            }
            else if (level.PortalAt(cell) is not null)
            {
                errors.Add(field, $"Leaky tile {cell} holds a portal; a portal cannot lie on a leaky tile");
            }

            if (!firstIndex.TryAdd(cell, index))
            {
                errors.Add(field, $"Leaky tile {cell} duplicates leaky[{firstIndex[cell]}]");
            }
        }
    }

    private static void CheckFlow(Errors errors, FlowSettings flow)
    {
        if (flow.InitialDelayMs < 0)
        {
            errors.Add("flow.initialDelayMs", $"flow.initialDelayMs must be 0 or greater, but had {flow.InitialDelayMs}");
        }

        if (!(flow.CellsPerSecond > 0.0 && flow.CellsPerSecond <= MaxCellsPerSecond))
        {
            errors.Add("flow.cellsPerSecond", $"flow.cellsPerSecond must be greater than 0 and at most {MaxCellsPerSecond}, but had {flow.CellsPerSecond}");
        }

        if (!(flow.ReservoirSpeedFactor > 0.0 && flow.ReservoirSpeedFactor <= 1.0))
        {
            errors.Add("flow.reservoirSpeedFactor", $"flow.reservoirSpeedFactor must be greater than 0 and at most 1, but had {flow.ReservoirSpeedFactor}");
        }

        if (!(flow.SpeedRampPerCell >= 0.0))
        {
            errors.Add("flow.speedRampPerCell", $"flow.speedRampPerCell must be 0 or greater, but had {flow.SpeedRampPerCell}");
        }
    }

    private static void CheckStack(Errors errors, StackSettings stack)
    {
        if (stack.VisibleSize is < MinVisibleStackSize or > MaxVisibleStackSize)
        {
            errors.Add("stack.visibleSize", $"stack.visibleSize must be between {MinVisibleStackSize} and {MaxVisibleStackSize}, but had {stack.VisibleSize}");
        }

        foreach (var section in PipeSectionExtensions.All)
        {
            var weight = stack.WeightOf(section);
            var field = $"stack.weights.{section}";
            if (weight < 0)
            {
                errors.Add(field, $"{field} must be 0 or greater, but had {weight}");
            }
            else if (section.IsAlwaysOnStack() && weight < 1)
            {
                errors.Add(field, $"{field} must be at least 1: straight and corner sections are always on the stack");
            }
        }
    }

    private static void CheckRules(Errors errors, PlacementRules rules)
    {
        if (rules.ReplacePenaltyMs < 0)
        {
            errors.Add("rules.replacePenaltyMs", $"rules.replacePenaltyMs must be 0 or greater, but had {rules.ReplacePenaltyMs}");
        }
    }

    private static void CheckStars(Errors errors, StarBenchmarks stars)
    {
        foreach (var (name, points) in new[] { ("one", stars.One), ("two", stars.Two), ("three", stars.Three) })
        {
            if (points < 1)
            {
                errors.Add($"stars.{name}", $"stars.{name} must be a positive number, but had {points}");
            }
        }

        if (!(stars.One < stars.Two && stars.Two < stars.Three))
        {
            errors.Add("stars", $"Star thresholds must be strictly increasing, but had one {stars.One}, two {stars.Two}, three {stars.Three}");
        }
    }

    private sealed class Errors
    {
        public List<LevelValidationError> List { get; } = [];

        public void Add(string field, string message) => List.Add(new LevelValidationError(field, message));
    }
}
