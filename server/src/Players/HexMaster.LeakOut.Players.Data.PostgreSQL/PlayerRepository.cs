using HexMaster.LeakOut.Players.DomainModels;
using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Players.Data.PostgreSQL;

public sealed class PlayerRepository(PlayersDbContext dbContext) : IPlayerRepository
{
    public async Task<Player?> FindBySubjectIdAsync(string subjectId, CancellationToken cancellationToken)
    {
        var entity = await dbContext.Players
            .AsNoTracking()
            .SingleOrDefaultAsync(p => p.SubjectId == subjectId, cancellationToken);

        return entity is null
            ? null
            : Player.Restore(entity.Id, entity.DisplayName, entity.CreatedAt, entity.LastSignedInAt);
    }

    public async Task AddAsync(string subjectId, Player player, CancellationToken cancellationToken)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(subjectId);
        ArgumentNullException.ThrowIfNull(player);

        dbContext.Players.Add(new PlayerEntity
        {
            Id = player.Id,
            SubjectId = subjectId,
            DisplayName = player.DisplayName,
            CreatedAt = player.CreatedAt,
            LastSignedInAt = player.LastSignedInAt,
        });
        await dbContext.SaveChangesAsync(cancellationToken);
    }

    public async Task UpdateAsync(Player player, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(player);

        var updated = await dbContext.Players
            .Where(p => p.Id == player.Id)
            .ExecuteUpdateAsync(
                setters => setters
                    .SetProperty(p => p.DisplayName, player.DisplayName)
                    .SetProperty(p => p.LastSignedInAt, player.LastSignedInAt),
                cancellationToken);

        if (updated == 0)
        {
            throw new InvalidOperationException($"Player {player.Id} does not exist.");
        }
    }
}
