using System.Text.Json;

namespace HexMaster.LeakOut.DailyPipes.Abstractions.DataTransferObjects;

/// <summary>
/// Today's daily pipe for a player: its UTC date, the level in the level-format JSON, the seed the
/// stack is dealt from, the player's streak and the player's best result on it, if any.
/// </summary>
public sealed record DailyPipeDto(DateOnly Date, JsonElement Level, long StackSeed, DailyStreakDto Streak, DailyBestScoreDto? BestScore);
