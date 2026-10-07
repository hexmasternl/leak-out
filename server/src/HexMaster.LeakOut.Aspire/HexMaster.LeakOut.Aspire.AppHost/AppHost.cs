var builder = DistributedApplication.CreateBuilder(args);

builder.AddProject<Projects.HexMaster_LeakOut_Api>("hexmaster-leakout-api");

builder.Build().Run();
