namespace HexMaster.LeakOut.DailyPipes.Features.GenerateDailyPipe;

/// <summary>Generates and publishes the daily pipe of today's UTC date, unless it already has one.</summary>
public sealed record GenerateDailyPipeCommand;

/// <summary>The date the run was for, whether this run published its pipe, and how many candidates it generated.</summary>
public sealed record GenerateDailyPipeResult(DateOnly Date, bool Published, int Attempts);

/// <summary>Thrown when no candidate passed the checks within <see cref="GenerateDailyPipeCommandHandler.MaxAttempts"/>.</summary>
public sealed class DailyPipeGenerationException(string message) : Exception(message);
