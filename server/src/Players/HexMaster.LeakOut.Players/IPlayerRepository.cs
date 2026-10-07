using HexMaster.LeakOut.Players.DomainModels;

namespace HexMaster.LeakOut.Players;

/// <summary>
/// Stores players. The Google Play Games player id (the subject id) is only known to this
/// port and its implementation; it never becomes part of the <see cref="Player"/> model.
/// </summary>
public interface IPlayerRepository
{
    Task<Player?> FindBySubjectIdAsync(string subjectId, CancellationToken cancellationToken);

    Task AddAsync(string subjectId, Player player, CancellationToken cancellationToken);

    Task UpdateAsync(Player player, CancellationToken cancellationToken);
}
