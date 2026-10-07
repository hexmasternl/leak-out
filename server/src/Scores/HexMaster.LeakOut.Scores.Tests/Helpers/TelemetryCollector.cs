using System.Collections.Concurrent;
using System.Diagnostics;
using System.Diagnostics.Metrics;
using HexMaster.LeakOut.Scores.Observability;
using Microsoft.Extensions.DependencyInjection;

namespace HexMaster.LeakOut.Scores.Tests.Helpers;

/// <summary>
/// Owns a <see cref="ScoresMetrics"/> instance and records the measurements and activities it
/// and the Scores module produce.
/// </summary>
public sealed class TelemetryCollector : IDisposable
{
    private readonly ServiceProvider _services;
    private readonly MeterListener _meterListener = new();
    private readonly ActivityListener _activityListener;

    public TelemetryCollector()
    {
        _services = new ServiceCollection().AddMetrics().BuildServiceProvider();
        var meterFactory = _services.GetRequiredService<IMeterFactory>();
        Metrics = new ScoresMetrics(meterFactory);

        _meterListener.InstrumentPublished = (instrument, listener) =>
        {
            if (ReferenceEquals(instrument.Meter.Scope, meterFactory))
            {
                listener.EnableMeasurementEvents(instrument);
            }
        };
        _meterListener.SetMeasurementEventCallback<long>((instrument, value, tags, _) =>
            Measurements.Add(new Measurement(instrument.Name, value, tags.ToArray().ToDictionary(t => t.Key, t => t.Value))));
        _meterListener.Start();

        _activityListener = new ActivityListener
        {
            ShouldListenTo = source => source.Name == ScoresMetrics.Name,
            Sample = (ref _) => ActivitySamplingResult.AllDataAndRecorded,
            ActivityStopped = Activities.Add,
        };
        ActivitySource.AddActivityListener(_activityListener);
    }

    public ScoresMetrics Metrics { get; }

    public ConcurrentBag<Measurement> Measurements { get; } = [];

    public ConcurrentBag<Activity> Activities { get; } = [];

    public void Dispose()
    {
        _activityListener.Dispose();
        _meterListener.Dispose();
        _services.Dispose();
    }

    public sealed record Measurement(string Instrument, long Value, IReadOnlyDictionary<string, object?> Tags);
}
