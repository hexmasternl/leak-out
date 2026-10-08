using HexMaster.LeakOut.Players.Data.PostgreSQL;
using HexMaster.LeakOut.Players.DomainModels;
using HexMaster.LeakOut.Players.Tests.Factories;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Players.Tests.Data;

/// <summary>Runs the repository against an in-memory SQLite database built from the EF model.</summary>
public sealed class PlayerRepositoryTests : IAsyncLifetime
{
    private const string SubjectId = "g1234567890";

    private readonly SqliteConnection _connection = new("DataSource=:memory:");

    public async ValueTask InitializeAsync()
    {
        await _connection.OpenAsync();
        await using var dbContext = CreateDbContext();
        await dbContext.Database.EnsureCreatedAsync();
    }

    public async ValueTask DisposeAsync() => await _connection.DisposeAsync();

    [Fact]
    public async Task AddAsync_StoresPlayerWithSubjectId()
    {
        var player = new PlayerFaker("Ann").Generate();

        await using (var dbContext = CreateDbContext())
        {
            await new PlayerRepository(dbContext).AddAsync(SubjectId, player, TestContext.Current.CancellationToken);
        }

        await using var verify = CreateDbContext();
        var stored = await verify.Players.SingleAsync(TestContext.Current.CancellationToken);
        Assert.Equal(player.Id, stored.Id);
        Assert.Equal(SubjectId, stored.SubjectId);
        Assert.Equal("Ann", stored.DisplayName);
        Assert.Equal(player.CreatedAt, stored.CreatedAt);
        Assert.Equal(player.LastSignedInAt, stored.LastSignedInAt);
    }

    [Fact]
    public async Task FindBySubjectIdAsync_ReturnsRestoredPlayer()
    {
        var player = new PlayerFaker("Ann").Generate();
        await AddAsync(SubjectId, player);

        await using var dbContext = CreateDbContext();
        var found = await new PlayerRepository(dbContext).FindBySubjectIdAsync(SubjectId, TestContext.Current.CancellationToken);

        Assert.NotNull(found);
        Assert.Equal(player.Id, found.Id);
        Assert.Equal("Ann", found.DisplayName);
        Assert.Equal(player.CreatedAt, found.CreatedAt);
        Assert.Equal(player.LastSignedInAt, found.LastSignedInAt);
    }

    [Fact]
    public async Task FindBySubjectIdAsync_UnknownSubjectId_ReturnsNull()
    {
        await AddAsync(SubjectId, new PlayerFaker().Generate());

        await using var dbContext = CreateDbContext();
        var found = await new PlayerRepository(dbContext).FindBySubjectIdAsync("g-unknown", TestContext.Current.CancellationToken);

        Assert.Null(found);
    }

    [Fact]
    public async Task UpdateAsync_StoresNewDisplayNameAndSignInTime()
    {
        var player = new PlayerFaker("Ann").Generate();
        await AddAsync(SubjectId, player);
        var signedInAt = player.LastSignedInAt.AddDays(3);
        player.RecordSignIn("Annie", signedInAt);

        await using (var dbContext = CreateDbContext())
        {
            await new PlayerRepository(dbContext).UpdateAsync(player, TestContext.Current.CancellationToken);
        }

        await using var verify = CreateDbContext();
        var stored = await verify.Players.SingleAsync(TestContext.Current.CancellationToken);
        Assert.Equal("Annie", stored.DisplayName);
        Assert.Equal(signedInAt, stored.LastSignedInAt);
        Assert.Equal(SubjectId, stored.SubjectId);
    }

    [Fact]
    public async Task UpdateAsync_UnknownPlayer_ThrowsInvalidOperationException()
    {
        await using var dbContext = CreateDbContext();

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => new PlayerRepository(dbContext).UpdateAsync(new PlayerFaker().Generate(), TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task AddAsync_SameSubjectIdTwice_IsRejectedByUniqueIndex()
    {
        await AddAsync(SubjectId, new PlayerFaker().Generate());

        await using var dbContext = CreateDbContext();
        await Assert.ThrowsAsync<DbUpdateException>(
            () => new PlayerRepository(dbContext).AddAsync(SubjectId, new PlayerFaker().Generate(), TestContext.Current.CancellationToken));
    }

    [Fact]
    public async Task GetDisplayNamesAsync_ReturnsNamesOfKnownPlayersOnly()
    {
        var ann = new PlayerFaker("Ann").Generate();
        var bob = new PlayerFaker("Bob").Generate();
        await AddAsync("g-ann", ann);
        await AddAsync("g-bob", bob);
        await AddAsync("g-cy", new PlayerFaker("Cy").Generate());
        var unknownId = Guid.CreateVersion7();

        await using var dbContext = CreateDbContext();
        var names = await new PlayerRepository(dbContext)
            .GetDisplayNamesAsync([ann.Id, bob.Id, unknownId], TestContext.Current.CancellationToken);

        Assert.Equal(2, names.Count);
        Assert.Equal("Ann", names[ann.Id]);
        Assert.Equal("Bob", names[bob.Id]);
        Assert.False(names.ContainsKey(unknownId));
    }

    [Fact]
    public async Task GetDisplayNamesAsync_NoIds_ReturnsEmpty()
    {
        await AddAsync(SubjectId, new PlayerFaker().Generate());

        await using var dbContext = CreateDbContext();
        var names = await new PlayerRepository(dbContext).GetDisplayNamesAsync([], TestContext.Current.CancellationToken);

        Assert.Empty(names);
    }

    private async Task AddAsync(string subjectId, Player player)
    {
        await using var dbContext = CreateDbContext();
        await new PlayerRepository(dbContext).AddAsync(subjectId, player, TestContext.Current.CancellationToken);
    }

    private PlayersDbContext CreateDbContext() =>
        new(new DbContextOptionsBuilder<PlayersDbContext>().UseSqlite(_connection).Options);
}
