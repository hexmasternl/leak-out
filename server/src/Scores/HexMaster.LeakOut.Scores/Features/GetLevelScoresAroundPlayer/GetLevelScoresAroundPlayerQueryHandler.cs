using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

namespace HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer;

/// <summary>
/// Returns up to 10 ranked scores of a level around the requesting player's best score: 5 above it,
/// the player's own and 4 below, shifted at the edges of the ranking. Empty when the player has no
/// score on the level.
/// </summary>
public sealed class GetLevelScoresAroundPlayerQueryHandler(
    ILevelScoreRepository repository,
    IPlayerProfileService playerProfiles) : IQueryHandler<GetLevelScoresAroundPlayerQuery, LevelLeaderboardDto>
{
    public const string UnknownPlayerName = "Unknown player";

    public async Task<LevelLeaderboardDto> Handle(GetLevelScoresAroundPlayerQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var best = await repository.GetBestAsync(query.UserId, query.Level, cancellationToken);
        if (best is null)
        {
            return new LevelLeaderboardDto(query.Level, []);
        }

        var rankedAbove = await repository.CountRankedAboveAsync(
            query.Level, best.Score, best.AchievedAt, query.UserId, cancellationToken);
        var total = await repository.CountAsync(query.Level, cancellationToken);

        // A score submitted between the reads can only push the total up, so the player's position always fits in it.
        var window = LeaderboardWindow.Compute(rankedAbove + 1, Math.Max(total, rankedAbove + 1));
        var ranked = await repository.GetRankedAsync(query.Level, window.Start - 1, window.Count, cancellationToken);

        var displayNames = await playerProfiles.GetDisplayNamesAsync(
            ranked.Select(s => s.UserId).ToArray(), cancellationToken);

        var entries = ranked
            .Select(s => new LeaderboardEntryDto(
                s.Position,
                displayNames.TryGetValue(s.UserId, out var name) && !string.IsNullOrWhiteSpace(name) ? name : UnknownPlayerName,
                s.Score,
                s.UserId == query.UserId))
            .ToList();

        return new LevelLeaderboardDto(query.Level, entries);
    }
}
