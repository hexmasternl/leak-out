using HexMaster.LeakOut.DailyPipes.Generation;

namespace HexMaster.LeakOut.DailyPipes.Tests.Generation;

public sealed class LevelJsonTests
{
    private const string Minimal = """
        {
          "id": 7,
          "name": "  Minimal  ",
          "grid": { "columns": 6, "rows": 5 },
          "start": { "x": 0, "y": 0, "openSide": "RIGHT" },
          "finish": { "x": 5, "y": 4, "openSide": "LEFT" },
          "blocked": [ { "x": 2, "y": 2 } ],
          "flow": { "initialDelayMs": 20000, "cellsPerSecond": 0.3 },
          "stars": { "one": 100, "two": 200, "three": 300 }
        }
        """;

    [Fact]
    public void Parse_FillsInTheDefaults()
    {
        var level = LevelJson.Parse(Minimal);

        Assert.Equal(7, level.Id);
        Assert.Equal("Minimal", level.Name);
        Assert.Equal(new GridSize(6, 5), level.Grid);
        Assert.Equal(new Endpoint(new Position(0, 0), Side.Right), level.Start);
        Assert.Equal(new Endpoint(new Position(5, 4), Side.Left), level.Finish);
        Assert.Equal([new Position(2, 2)], level.Blocked);
        Assert.Equal(new FlowSettings(20000, 0.3), level.Flow);
        Assert.Equal(LevelDefaults.VisibleStackSize, level.Stack.VisibleSize);
        Assert.All(PipeSectionExtensions.All, s => Assert.Equal(LevelDefaults.StackWeights[s], level.Stack.WeightOf(s)));
        Assert.Equal(new PlacementRules(), level.Rules);
        Assert.Equal(new StarBenchmarks(100, 200, 300), level.Stars);
        Assert.Empty(level.Mandatory);
        Assert.Empty(level.Portals);
        Assert.Empty(level.Leaky);
    }

    [Fact]
    public void Parse_ReadsEveryOptionalField()
    {
        var level = LevelJson.Parse(File.ReadAllText(Path.Combine(Fixtures.Directory, "levels", "level_040.json")));

        Assert.Equal(6, level.Stack.WeightOf(PipeSection.X));
        Assert.Equal(2, level.Stack.WeightOf(PipeSection.RES));
        Assert.Equal(new MandatorySection(new Position(2, 6), PipeSection.X), level.Mandatory[0]);
        Assert.Equal(new MandatorySection(new Position(6, 4), PipeSection.RES), level.Mandatory[1]);
    }

    [Fact]
    public void Write_ThenParse_GivesTheSameLevelForEveryFixture()
    {
        foreach (var file in Fixtures.LevelFiles())
        {
            var written = LevelJson.Write(LevelJson.Parse(File.ReadAllText(file)));

            Assert.Equal(written, LevelJson.Write(LevelJson.Parse(written)));
        }
    }

    [Fact]
    public void Write_SpellsSidesAndSectionsAsTheLevelFormat()
    {
        var level = LevelJson.Parse(File.ReadAllText(Path.Combine(Fixtures.Directory, "levels", "solvable_through_portals.json")));

        var json = LevelJson.Write(level);

        Assert.Contains("\"openSide\": \"RIGHT\"", json, StringComparison.Ordinal);
        Assert.Contains("\"RES\": 0", json, StringComparison.Ordinal);
        Assert.Contains("\"portals\"", json, StringComparison.Ordinal);
        Assert.DoesNotContain("\"mandatory\"", json, StringComparison.Ordinal);
    }

    [Theory]
    [InlineData("not json", "not valid JSON")]
    [InlineData("[]", "expected an object")]
    [InlineData("""{"id":1}""", "Missing required field")]
    public void Parse_Malformed_Throws(string json, string message)
    {
        var exception = Assert.Throws<LevelFormatException>(() => LevelJson.Parse(json));

        Assert.Contains(message, exception.Message, StringComparison.Ordinal);
    }

    [Theory]
    [InlineData("\"blocked\": [ { \"x\": 2, \"y\": 2 } ]", "\"blocked\": [ { \"x\": 2, \"y\": 2, \"z\": 1 } ]", "Unknown field 'blocked[0].z'")]
    [InlineData("\"id\": 7", "\"id\": 7, \"extra\": true", "Unknown field 'extra'")]
    [InlineData("\"openSide\": \"RIGHT\"", "\"openSide\": \"right\"", "Invalid value 'right' for 'start.openSide'")]
    [InlineData("\"id\": 7", "\"id\": \"7\"", "Invalid value for 'id'")]
    [InlineData("\"initialDelayMs\": 20000", "\"initialDelayMs\": 2.5", "Invalid value for 'flow.initialDelayMs'")]
    [InlineData("\"cellsPerSecond\": 0.3", "\"cellsPerSecond\": \"fast\"", "Invalid value for 'flow.cellsPerSecond'")]
    [InlineData("\"name\": \"  Minimal  \"", "\"name\": 5", "Invalid value for 'name'")]
    [InlineData("\"blocked\": [ { \"x\": 2, \"y\": 2 } ]", "\"blocked\": {}", "Invalid value for 'blocked'")]
    [InlineData("\"blocked\": [ { \"x\": 2, \"y\": 2 } ],", "\"blocked\": [], \"rules\": { \"allowReplace\": \"true\" },", "Invalid value for 'rules.allowReplace'")]
    [InlineData("\"blocked\": [ { \"x\": 2, \"y\": 2 } ],", "\"blocked\": [], \"stack\": { \"weights\": { \"Q\": 1 } },", "Unknown field 'stack.weights.Q'")]
    [InlineData("\"blocked\": [ { \"x\": 2, \"y\": 2 } ],", "\"blocked\": [], \"mandatory\": [ { \"x\": 1, \"y\": 1, \"section\": \"Z\" } ],", "expected one of H, V, LT, LB, RT, RB, X, RES")]
    public void Parse_WrongField_NamesIt(string original, string replacement, string message)
    {
        var json = Minimal.Replace(original, replacement, StringComparison.Ordinal);
        Assert.NotEqual(Minimal, json);

        var exception = Assert.Throws<LevelFormatException>(() => LevelJson.Parse(json));

        Assert.Contains(message, exception.Message, StringComparison.Ordinal);
    }

    [Fact]
    public void Parse_ReadsRulesAndStackSettings()
    {
        var json = Minimal.Replace(
            "\"blocked\": [ { \"x\": 2, \"y\": 2 } ],",
            "\"blocked\": [], \"rules\": { \"allowReplace\": false, \"replacePenaltyMs\": 900 }, \"stack\": { \"visibleSize\": 3 }, \"leaky\": [ { \"x\": 1, \"y\": 1 } ],",
            StringComparison.Ordinal);

        var level = LevelJson.Parse(json);

        Assert.Equal(new PlacementRules(false, 900), level.Rules);
        Assert.Equal(3, level.Stack.VisibleSize);
        Assert.Equal([new Position(1, 1)], level.Leaky);
    }
}
