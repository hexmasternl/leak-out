namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>A daily pipe ready to publish: the checked level, its JSON, the stack seed and the highest score it allows.</summary>
public sealed record BuiltDailyPipe(Level Level, string LevelJson, long StackSeed, int MaxScore);

/// <summary>
/// Turns a <see cref="DailyPipeCandidate"/> into a publishable daily pipe, or rejects it: the
/// candidate must pass <see cref="LevelRules"/> and <see cref="LevelSolver"/>. The solver's route
/// then sets the head start and the star thresholds, and the finished level is checked again.
/// </summary>
public static class DailyPipeBuilder
{
    public const double OneStarFactor = 0.6;
    public const double TwoStarFactor = 0.9;
    public const double ThreeStarFactor = 1.2;

    // The app's Points values (core/.../score/Points.kt).
    public const int StraightPoints = 10;
    public const int CornerPoints = 15;
    public const int ReservoirPoints = 25;
    public const int CrossFirstPoints = 10;
    public const int CrossSecondPoints = 50;
    public const int LeakyTilePenalty = 20;

    /// <summary>The checked daily pipe, or <see langword="null"/> when the candidate is invalid or unsolvable.</summary>
    public static BuiltDailyPipe? TryBuild(DailyPipeCandidate candidate)
    {
        ArgumentNullException.ThrowIfNull(candidate);

        if (LevelRules.Validate(candidate.Level).Count > 0
            || LevelSolver.Solve(candidate.Level) is not SolverResult.Solvable solvable)
        {
            return null;
        }

        var points = RoutePoints(candidate.Level, solvable.Path);
        var level = candidate.Level with
        {
            Flow = candidate.Level.Flow with
            {
                InitialDelayMs = candidate.Level.Flow.InitialDelayMs + (candidate.DelayPerRouteCellMs * solvable.Path.Count),
            },
            Stars = StarsFor(points),
        };
        if (LevelRules.Validate(level).Count > 0)
        {
            return null;
        }

        return new BuiltDailyPipe(level, LevelJson.Write(level), candidate.StackSeed, MaxScore(level));
    }

    /// <summary>
    /// The points the liquid earns along <paramref name="path"/> with a straight or corner on every
    /// ordinary cell, as the app scores it: a reservoir 25, a cross 10 on its first pass and 50 on its
    /// second, a leaky tile the leak penalty. Portals earn nothing.
    /// </summary>
    public static int RoutePoints(Level level, IReadOnlyList<Position> path)
    {
        ArgumentNullException.ThrowIfNull(level);
        ArgumentNullException.ThrowIfNull(path);

        var crossPasses = new Dictionary<Position, int>();
        var points = 0;
        for (var i = 0; i < path.Count; i++)
        {
            var cell = path[i];
            if (level.PortalAt(cell) is not null)
            {
                continue;
            }

            var mandatory = level.MandatoryAt(cell);
            if (mandatory?.Section == PipeSection.X)
            {
                var pass = crossPasses[cell] = crossPasses.GetValueOrDefault(cell) + 1;
                points += pass >= 2 ? CrossSecondPoints : CrossFirstPoints;
            }
            else if (mandatory?.Section == PipeSection.RES)
            {
                points += ReservoirPoints;
            }
            else if (level.IsLeaky(cell))
            {
                points -= LeakyTilePenalty;
            }
            else
            {
                var previous = i == 0 ? level.Start.Position : path[i - 1];
                var next = i == path.Count - 1 ? level.Finish.Position : path[i + 1];
                var straight = previous.X == next.X || previous.Y == next.Y;
                points += straight ? StraightPoints : CornerPoints;
            }
        }

        return points;
    }

    /// <summary>The thresholds for a route worth <paramref name="routePoints"/>, rounded to tens and strictly increasing.</summary>
    public static StarBenchmarks StarsFor(int routePoints)
    {
        var one = Math.Max(10, RoundToTen(routePoints * OneStarFactor));
        var two = Math.Max(one + 10, RoundToTen(routePoints * TwoStarFactor));
        var three = Math.Max(two + 10, RoundToTen(routePoints * ThreeStarFactor));
        return new StarBenchmarks(one, two, three);
    }

    /// <summary>
    /// The highest score a submission may claim: every scoring cell holding a cross the liquid passes
    /// twice. No real attempt gets near it; it only rules out impossible scores.
    /// </summary>
    public static int MaxScore(Level level)
    {
        ArgumentNullException.ThrowIfNull(level);
        return Math.Max(1, level.ScoringCells * (CrossFirstPoints + CrossSecondPoints));
    }

    private static int RoundToTen(double value) => (int)Math.Round(value / 10, MidpointRounding.AwayFromZero) * 10;
}
