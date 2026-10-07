namespace HexMaster.LeakOut.Core;

/// <summary>Handles a query and returns its result.</summary>
public interface IQueryHandler<in TQuery, TResult>
{
    Task<TResult> Handle(TQuery query, CancellationToken cancellationToken);
}
