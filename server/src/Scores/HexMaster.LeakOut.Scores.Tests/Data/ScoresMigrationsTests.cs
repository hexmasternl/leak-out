using HexMaster.LeakOut.Scores.Data.PostgreSQL;
using HexMaster.LeakOut.Scores.Tests.Helpers;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Scores.Tests.Data;

[Collection(PostgreSqlCollection.Name)]
public sealed class ScoresMigrationsTests(PostgreSqlFixture database)
{
    [Fact]
    public void Model_HasNoChangesMissingFromMigrations()
    {
        using var dbContext = new ScoresDbContextDesignTimeFactory().CreateDbContext([]);

        Assert.False(
            dbContext.Database.HasPendingModelChanges(),
            "The EF model changed without a migration; run 'dotnet ef migrations add'.");
        Assert.Contains(dbContext.Database.GetMigrations(), m => m.EndsWith("_InitialCreate", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Migrator_CreatesTableWithConstraintsAndIndexes()
    {
        await using var dbContext = database.CreateDbContext();

        Assert.Empty(await dbContext.Database.GetPendingMigrationsAsync(TestContext.Current.CancellationToken));

        var constraints = await dbContext.Database
            .SqlQuery<string>($"""
                SELECT conname || ': ' || pg_get_constraintdef(oid) AS "Value"
                FROM pg_constraint WHERE conrelid = 'level_scores'::regclass
                """)
            .ToListAsync(TestContext.Current.CancellationToken);
        Assert.Contains(constraints, c => c.StartsWith("pk_level_scores: PRIMARY KEY (id)", StringComparison.Ordinal));
        Assert.Contains(constraints, c => c.StartsWith("ck_level_scores_level: CHECK ((level >= 1))", StringComparison.Ordinal));
        Assert.Contains(constraints, c => c.StartsWith("ck_level_scores_score: CHECK ((score >= 0))", StringComparison.Ordinal));

        var indexes = await dbContext.Database
            .SqlQuery<string>($"""SELECT indexdef AS "Value" FROM pg_indexes WHERE tablename = 'level_scores'""")
            .ToListAsync(TestContext.Current.CancellationToken);
        Assert.Contains(indexes, i => i.Contains("UNIQUE INDEX ix_level_scores_user_id_level", StringComparison.Ordinal)
            && i.EndsWith("(user_id, level)", StringComparison.Ordinal));
        Assert.Contains(indexes, i => i.Contains("INDEX ix_level_scores_level_score_achieved_at", StringComparison.Ordinal)
            && i.EndsWith("(level, score DESC, achieved_at)", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Migrator_StopAsync_DoesNothing()
    {
        var migrator = new ScoresDatabaseMigrator(null!);

        await migrator.StopAsync(TestContext.Current.CancellationToken);
    }
}
