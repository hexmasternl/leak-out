using HexMaster.LeakOut.Players.Abstractions.Services;
using Microsoft.Extensions.Caching.Memory;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>
/// Looks up the internal player id for a subject id and caches hits. Misses are not cached, so a
/// player who registered a moment ago resolves on their next request.
/// </summary>
public sealed class UserResolver(IPlayerRepository playerRepository, IMemoryCache cache) : IUserResolver
{
    public static readonly TimeSpan SlidingExpiration = TimeSpan.FromMinutes(10);

    public async Task<Guid?> ResolveAsync(string subjectId, CancellationToken cancellationToken)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(subjectId);

        var cacheKey = $"players:subject:{subjectId}";
        if (cache.TryGetValue(cacheKey, out Guid playerId))
        {
            return playerId;
        }

        var player = await playerRepository.FindBySubjectIdAsync(subjectId, cancellationToken);
        if (player is null)
        {
            return null;
        }

        cache.Set(cacheKey, player.Id, new MemoryCacheEntryOptions { SlidingExpiration = SlidingExpiration });
        return player.Id;
    }
}
