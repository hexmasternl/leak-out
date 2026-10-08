namespace HexMaster.LeakOut.Core;

/// <summary>Handles a command that produces a result.</summary>
public interface ICommandHandler<in TCommand, TResult>
{
    Task<TResult> Handle(TCommand command, CancellationToken cancellationToken);
}

/// <summary>Handles a command that produces no result.</summary>
public interface ICommandHandler<in TCommand>
{
    Task Handle(TCommand command, CancellationToken cancellationToken);
}
