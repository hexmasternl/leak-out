using System.Globalization;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace HexMaster.LeakOut.DailyPipes.Generation;

/// <summary>Thrown when level JSON does not have the shape of <c>docs/level-format.md</c>.</summary>
public sealed class LevelFormatException(string message) : Exception(message);

/// <summary>
/// Reads and writes levels in the level-format JSON (<c>docs/level-format.md</c>), the contract with
/// the app's <c>LevelParser</c>. Reading checks shape and types only and, like the app, rejects unknown
/// fields; optional fields get the <see cref="LevelDefaults"/>. Whether the values make a playable
/// level is <see cref="LevelRules"/>' job.
/// </summary>
public static class LevelJson
{
    private static readonly JsonSerializerOptions WriteOptions = new() { WriteIndented = true };

    public static Level Parse(string json)
    {
        JsonElement root;
        try
        {
            using var document = JsonDocument.Parse(json);
            root = document.RootElement.Clone();
        }
        catch (JsonException ex)
        {
            throw new LevelFormatException($"The level is not valid JSON: {ex.Message}");
        }

        return ReadLevel(root);
    }

    /// <summary>Writes every field explicitly, so the app reads the level without relying on its defaults.</summary>
    public static string Write(Level level)
    {
        ArgumentNullException.ThrowIfNull(level);

        var root = new JsonObject
        {
            ["id"] = level.Id,
            ["name"] = level.Name,
            ["grid"] = new JsonObject { ["columns"] = level.Grid.Columns, ["rows"] = level.Grid.Rows },
            ["start"] = WriteEndpoint(level.Start),
            ["finish"] = WriteEndpoint(level.Finish),
            ["blocked"] = WriteCells(level.Blocked),
            ["flow"] = new JsonObject
            {
                ["initialDelayMs"] = level.Flow.InitialDelayMs,
                ["cellsPerSecond"] = level.Flow.CellsPerSecond,
                ["reservoirSpeedFactor"] = level.Flow.ReservoirSpeedFactor,
                ["speedRampPerCell"] = level.Flow.SpeedRampPerCell,
            },
            ["stack"] = new JsonObject
            {
                ["visibleSize"] = level.Stack.VisibleSize,
                ["weights"] = new JsonObject(PipeSectionExtensions.All.Select(s =>
                    KeyValuePair.Create(s.ToString(), (JsonNode?)level.Stack.WeightOf(s)))),
            },
            ["rules"] = new JsonObject
            {
                ["allowReplace"] = level.Rules.AllowReplace,
                ["replacePenaltyMs"] = level.Rules.ReplacePenaltyMs,
            },
            ["stars"] = new JsonObject { ["one"] = level.Stars.One, ["two"] = level.Stars.Two, ["three"] = level.Stars.Three },
        };
        if (level.Mandatory.Count > 0)
        {
            root["mandatory"] = new JsonArray(level.Mandatory
                .Select(m => (JsonNode)new JsonObject { ["x"] = m.Position.X, ["y"] = m.Position.Y, ["section"] = m.Section.ToString() })
                .ToArray());
        }

        if (level.Portals.Count > 0)
        {
            root["portals"] = new JsonArray(level.Portals
                .Select(p => (JsonNode)new JsonObject { ["a"] = WriteEndpoint(p.A), ["b"] = WriteEndpoint(p.B) })
                .ToArray());
        }

        if (level.Leaky.Count > 0)
        {
            root["leaky"] = WriteCells(level.Leaky);
        }

        return root.ToJsonString(WriteOptions);
    }

    private static JsonObject WriteEndpoint(Endpoint endpoint) => new()
    {
        ["x"] = endpoint.Position.X,
        ["y"] = endpoint.Position.Y,
        ["openSide"] = endpoint.OpenSide.ToJsonName(),
    };

    private static JsonArray WriteCells(IEnumerable<Position> cells) =>
        new(cells.Select(c => (JsonNode)new JsonObject { ["x"] = c.X, ["y"] = c.Y }).ToArray());

    private static Level ReadLevel(JsonElement root)
    {
        var level = new Reader(root, string.Empty, "id", "name", "grid", "start", "finish", "blocked", "flow", "stack", "rules", "stars", "mandatory", "portals", "leaky");
        var grid = level.Object("grid", "columns", "rows");
        var flow = level.Object("flow", "initialDelayMs", "cellsPerSecond", "reservoirSpeedFactor", "speedRampPerCell");
        var stack = level.OptionalObject("stack", "visibleSize", "weights");
        var weights = stack?.OptionalObject("weights", PipeSectionExtensions.All.Select(s => s.ToString()).ToArray());
        var rules = level.OptionalObject("rules", "allowReplace", "replacePenaltyMs");
        var stars = level.Object("stars", "one", "two", "three");

        return new Level(
            level.Int("id"),
            level.String("name").Trim(),
            new GridSize(grid.Int("columns"), grid.Int("rows")),
            ReadEndpoint(level.Object("start", "x", "y", "openSide")),
            ReadEndpoint(level.Object("finish", "x", "y", "openSide")),
            level.Array("blocked").Select(ReadCell).ToList(),
            new FlowSettings(
                flow.Long("initialDelayMs"),
                flow.Double("cellsPerSecond"),
                flow.OptionalDouble("reservoirSpeedFactor") ?? LevelDefaults.ReservoirSpeedFactor,
                flow.OptionalDouble("speedRampPerCell") ?? LevelDefaults.SpeedRampPerCell),
            new StackSettings(
                stack?.OptionalInt("visibleSize") ?? LevelDefaults.VisibleStackSize,
                PipeSectionExtensions.All.ToDictionary(s => s, s => weights?.OptionalInt(s.ToString()) ?? LevelDefaults.StackWeights[s])),
            new PlacementRules(
                rules?.OptionalBool("allowReplace") ?? LevelDefaults.AllowReplace,
                rules?.OptionalLong("replacePenaltyMs") ?? LevelDefaults.ReplacePenaltyMs),
            new StarBenchmarks(stars.Int("one"), stars.Int("two"), stars.Int("three")),
            level.OptionalArray("mandatory").Select(m =>
            {
                var cell = m.WithKeys("x", "y", "section");
                return new MandatorySection(new Position(cell.Int("x"), cell.Int("y")), cell.Enum<PipeSection>("section"));
            }).ToList(),
            level.OptionalArray("portals").Select(p =>
            {
                var pair = p.WithKeys("a", "b");
                return new PortalPair(ReadEndpoint(pair.Object("a", "x", "y", "openSide")), ReadEndpoint(pair.Object("b", "x", "y", "openSide")));
            }).ToList(),
            level.OptionalArray("leaky").Select(ReadCell).ToList());
    }

    private static Endpoint ReadEndpoint(Reader endpoint) =>
        new(new Position(endpoint.Int("x"), endpoint.Int("y")), endpoint.Enum<Side>("openSide"));

    private static Position ReadCell(Reader cell)
    {
        var checkedCell = cell.WithKeys("x", "y");
        return new Position(checkedCell.Int("x"), checkedCell.Int("y"));
    }

    /// <summary>Reads the fields of one JSON object, naming the dotted path of a field in every error.</summary>
    private sealed class Reader
    {
        private readonly JsonElement _element;
        private readonly string _path;

        public Reader(JsonElement element, string path, params string[] allowedKeys)
        {
            if (element.ValueKind != JsonValueKind.Object)
            {
                throw new LevelFormatException($"Invalid value for '{Display(path)}': expected an object");
            }

            _element = element;
            _path = path;
            if (allowedKeys.Length > 0)
            {
                foreach (var property in element.EnumerateObject())
                {
                    if (!allowedKeys.Contains(property.Name, StringComparer.Ordinal))
                    {
                        throw new LevelFormatException($"Unknown field '{Join(path, property.Name)}'");
                    }
                }
            }
        }

        public Reader WithKeys(params string[] allowedKeys) => new(_element, _path, allowedKeys);

        public Reader Object(string name, params string[] allowedKeys) =>
            new(Required(name), Join(_path, name), allowedKeys);

        public Reader? OptionalObject(string name, params string[] allowedKeys) =>
            _element.TryGetProperty(name, out var value) ? new Reader(value, Join(_path, name), allowedKeys) : null;

        public IEnumerable<Reader> Array(string name) => Items(name, Required(name));

        public IEnumerable<Reader> OptionalArray(string name) =>
            _element.TryGetProperty(name, out var value) ? Items(name, value) : [];

        public int Int(string name) => ReadInt(name, Required(name));

        public int? OptionalInt(string name) => _element.TryGetProperty(name, out var value) ? ReadInt(name, value) : null;

        public long Long(string name) => ReadLong(name, Required(name));

        public long? OptionalLong(string name) => _element.TryGetProperty(name, out var value) ? ReadLong(name, value) : null;

        public double Double(string name) => ReadDouble(name, Required(name));

        public double? OptionalDouble(string name) => _element.TryGetProperty(name, out var value) ? ReadDouble(name, value) : null;

        public bool? OptionalBool(string name)
        {
            if (!_element.TryGetProperty(name, out var value))
            {
                return null;
            }

            return value.ValueKind switch
            {
                JsonValueKind.True => true,
                JsonValueKind.False => false,
                _ => throw Invalid(name, "expected true or false"),
            };
        }

        public string String(string name)
        {
            var value = Required(name);
            return value.ValueKind == JsonValueKind.String ? value.GetString()! : throw Invalid(name, "expected a string");
        }

        public TEnum Enum<TEnum>(string name)
            where TEnum : struct, Enum
        {
            var text = String(name);
            foreach (var candidate in System.Enum.GetValues<TEnum>())
            {
                if (JsonName(candidate) == text)
                {
                    return candidate;
                }
            }

            var allowed = string.Join(", ", System.Enum.GetValues<TEnum>().Select(JsonName));
            throw new LevelFormatException($"Invalid value '{text}' for '{Join(_path, name)}'; expected one of {allowed}");
        }

        private static string JsonName<TEnum>(TEnum value)
            where TEnum : struct, Enum =>
            value is Side side ? side.ToJsonName() : value.ToString();

        private IEnumerable<Reader> Items(string name, JsonElement value)
        {
            if (value.ValueKind != JsonValueKind.Array)
            {
                throw Invalid(name, "expected an array");
            }

            var path = Join(_path, name);
            return value.EnumerateArray()
                .Select((item, index) => new Reader(item, string.Create(CultureInfo.InvariantCulture, $"{path}[{index}]")))
                .ToList();
        }

        private JsonElement Required(string name) =>
            _element.TryGetProperty(name, out var value) && value.ValueKind != JsonValueKind.Null
                ? value
                : throw new LevelFormatException($"Missing required field '{Join(_path, name)}'");

        private int ReadInt(string name, JsonElement value) =>
            value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var number) ? number : throw Invalid(name, "expected a whole number");

        private long ReadLong(string name, JsonElement value) =>
            value.ValueKind == JsonValueKind.Number && value.TryGetInt64(out var number) ? number : throw Invalid(name, "expected a whole number");

        private double ReadDouble(string name, JsonElement value) =>
            value.ValueKind == JsonValueKind.Number ? value.GetDouble() : throw Invalid(name, "expected a number");

        private LevelFormatException Invalid(string name, string expected) =>
            new($"Invalid value for '{Join(_path, name)}': {expected}");

        private static string Join(string path, string name) => path.Length == 0 ? name : $"{path}.{name}";

        private static string Display(string path) => path.Length == 0 ? "$" : path;
    }
}
