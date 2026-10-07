using HexMaster.LeakOut.Core;
using HexMaster.LeakOut.Players.Abstractions.Services;
using HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects;

namespace HexMaster.LeakOut.Scores.Features.GetLevelTopScores;

/// <summary>
/// Returns the 10 best scores of a level in ranking order, with the players' display names and the
/// requesting player's entry marked.
/// </summary>
public sealed class GetLevelTopScoresQueryHandler(
    ILevelScoreRepository repository,
    IPlayerProfileService playerProfiles) : IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto>
{
    public const int TopCount = 10;
    public const string UnknownPlayerName = "Unknown player";

    public async Task<LevelLeaderboardDto> Handle(GetLevelTopScoresQuery query, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(query);

        var ranked = await repository.GetTopAsync(query.Level, TopCount, cancellationToken);
        if (ranked.Count == 0)
        {
            return new LevelLeaderboardDto(query.Level, []);
        }

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
